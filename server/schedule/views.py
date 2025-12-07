import json
import logging
from datetime import timedelta
from django.db import transaction
from django.db.models import F
from django.shortcuts import get_object_or_404
from django.conf import settings
from django.core.exceptions import ValidationError
from datetime import datetime, time
from django.utils import timezone
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from openai import OpenAI, OpenAIError
from rest_framework.request import Request

from .models import Schedule, Session, Feedback, DailySummary, ActivityType, ACTIVITY_TYPE_MAP
from .serializers import ScheduleSerializer, SessionSerializer, DailySummarySerializer
from .tasks import mark_missed_schedules

from django.utils.dateparse import parse_date, parse_time
from .services.schedule_ai_service import generate_daily_schedules, AIScheduleError
from .services.daily_summary_ai_service import (
    generate_daily_summaries_for_user, DailySummaryError, APIResponseError, GenerationError
)

logger = logging.getLogger(__name__)
client = OpenAI(api_key=settings.OPENAI_API_KEY)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def schedules_view(request):
    user = request.user
    status_filter = request.query_params.get('status')
    schedules = Schedule.objects.filter(user=user).order_by('-scheduled_date')
    if status_filter:
        schedules = schedules.filter(status=status_filter)
    serializer = ScheduleSerializer(schedules, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

AUTO_GENERATE_DAYS = 4

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def schedules_auto_generate(request):
    user = request.user
    today = timezone.localdate()
    now_time = timezone.localtime().time()

    existing_dates = Schedule.objects.filter(
        user=user,
        scheduled_date__gte=today,
        scheduled_date__lte=today + timedelta(days=AUTO_GENERATE_DAYS-1)
    ).values_list('scheduled_date', flat=True)

    missing_dates = [
        today + timedelta(days=i)
        for i in range(AUTO_GENERATE_DAYS)
        if (today + timedelta(days=i)) not in existing_dates
    ]

    created_schedules = []
    errors = []

    for date in missing_dates:
        try:
            ai_schedules = generate_daily_schedules(user, date)
            
            if 'schedules' not in ai_schedules or not isinstance(ai_schedules['schedules'], list):
                errors.append({"date": str(date), "error": "AI output missing 'schedules' key or not a list"})
                continue

            valid_schedules = []
            for s in ai_schedules['schedules']:
                required_fields = ["scheduled_date", "start_time", "end_time", "activity"]
                if not all(field in s for field in required_fields):
                    logger.warning(f"Missing required field in AI schedule: {s}")
                    continue

                try:
                    s_date = datetime.strptime(s['scheduled_date'], "%Y-%m-%d").date()
                    s_start = datetime.strptime(s['start_time'], "%H:%M").time()
                except ValueError as ve:
                    logger.warning(f"Invalid date/time format in AI schedule: {s}")
                    continue

                if s_date > today or (s_date == today and s_start >= now_time):
                    valid_schedules.append(s)

            if not valid_schedules:
                continue

            serializer = ScheduleSerializer(data=valid_schedules, many=True)
            try:
                serializer.is_valid(raise_exception=True)
            except ValidationError as ve:
                logger.warning(f"Serializer validation failed for date {date}: {ve.detail}")
                errors.append({"date": str(date), "error": ve.detail})
                continue

            serializer.save(user=user)
            created_schedules.extend(serializer.data)

        except AIScheduleError as e:
            cause = e.__cause__
            logger.error(f"AIScheduleError on date {date}: {str(e)}")
            if isinstance(cause, json.JSONDecodeError):
                errors.append({"date": str(date), "error": "AI returned invalid JSON"})
            elif isinstance(cause, OpenAIError):
                errors.append({"date": str(date), "error": "AI service unavailable"})
            else:
                errors.append({"date": str(date), "error": str(e)})

        except Exception as e:
            logger.exception(f"Unexpected error on date {date}: {str(e)}")
            errors.append({"date": str(date), "error": "Unexpected error occurred"})

    return Response(
        {
            "created_count": len(created_schedules),
            "dates_generated": list({s['scheduled_date'] for s in created_schedules}),
            "errors": errors
        },
        status=status.HTTP_201_CREATED
    )

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def mark_missed_schedules_view(request):
    try:
        mark_missed_schedules()
        return Response({"detail": "Missed schedules processed"}, status=status.HTTP_200_OK)
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def sessions_view(request):
    user = request.user
    sessions = Session.objects.filter(user=user).order_by('-created_at')
    serializer = SessionSerializer(sessions, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def start_session(request):
    user = request.user
    activity = request.data.get('activity')
    if activity not in [a.value for a in ActivityType]:
        return Response({"error": "Invalid activity type"}, status=HTTP_400_BAD_REQUEST)
    schedule_id = request.data.get('schedule_id')

    session_data = {
        "user": user,
        "activity": activity,
    }

    if schedule_id:
        try:
            schedule = Schedule.objects.get(id=schedule_id, user=user)
            session_data["schedule"] = schedule
        except Schedule.DoesNotExist:
            return Response({"error": "Invalid schedule_id"}, status=status.HTTP_400_BAD_REQUEST)

    session = Session.objects.create(**session_data)
    serializer = SessionSerializer(session)
    return Response(serializer.data, status=status.HTTP_201_CREATED)

@api_view(['PATCH'])
@permission_classes([IsAuthenticated])
def end_session(request, session_id):
    user = request.user
    session = get_object_or_404(Session, id=session_id, user=user)

    reps_count = request.data.get('reps_count')
    duration_seconds = request.data.get('duration')
    session_duration_seconds = request.data.get('session_duration_seconds')
    duration = timedelta(seconds=duration_seconds) if duration_seconds is not None else None

    if reps_count is not None:
        session.reps_count = reps_count
    if duration is not None:
        session.duration = duration

    try:
        session.full_clean()
    except ValidationError as e:
        return Response({"error": e.message_dict}, status=status.HTTP_400_BAD_REQUEST)

    session.save()

    if reps_count is not None:
        user.xp += int(reps_count) * 10
    elif duration is not None:
        user.xp += int(duration.total_seconds())

    if session_duration_seconds is not None:
        try:
            user.total_time += float(session_duration_seconds)
        except ValueError:
            return Response({"error": "Invalid session_duration_seconds"}, status.HTTP_400_BAD_REQUEST)

        user.last_session_at = timezone.now()

    user.save()

    if session.schedule:
        schedule = session.schedule

        if reps_count is not None:
            schedule.reps_done = reps_count
        if duration is not None:
            schedule.duration_done = duration

        if ((schedule.reps_target and schedule.reps_done >= schedule.reps_target) or
            (schedule.duration_target and schedule.duration_done >= schedule.duration_target)):
            schedule.status = 'completed'
        elif ((schedule.reps_done and schedule.reps_done > 0) or
              (schedule.duration_done and schedule.duration_done.total_seconds() > 0)):
            schedule.status = 'partial'

        schedule.save()

    serializer = SessionSerializer(session)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def daily_summaries_view(request):
    user = request.user
    daily_summaries = DailySummary.objects.filter(user=user).order_by('-date')
    serializer = DailySummarySerializer(daily_summaries, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def daily_summaries_auto_generate(request):
    user = request.user

    try:
        summaries = generate_daily_summaries_for_user(user)
        if not summaries:
            return Response({"message": "No new daily summaries needed."}, status=status.HTTP_200_OK)
        
        return Response(
            {
                "message": "Daily summaries generated successfully.",
                "count": len(summaries)
            },
            status=status.HTTP_201_CREATED
        )

    except APIResponseError as e:
        return Response({"error": str(e)}, status=status.HTTP_502_BAD_GATEWAY)

    except DailySummaryError as e:
        return Response({"error": str(e)}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

    except GenerationError as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
