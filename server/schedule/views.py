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
from rest_framework.test import APIRequestFactory, force_authenticate

from .models import Schedule, Session, Feedback, DailySummary, ActivityType, ACTIVITY_TYPE_MAP
from .serializers import ScheduleSerializer, SessionSerializer, FeedbackSerializer, DailySummarySerializer
from .utils.feedback import generate_feedback_from_schedule
from .tasks import mark_missed_schedules, generate_daily_summaries_for_user

from django.utils.dateparse import parse_date, parse_time

logger = logging.getLogger(__name__)
client = OpenAI(api_key=settings.OPENAI_API_KEY)

# -----------------------------------
# 🟢 Session Start / End
# -----------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def start_session(request):
    user = request.user
    activity = request.data.get('activity')
    if activity not in [a.value for a in ActivityType]:
        return Response({"error": "Invalid activity type"}, status=400)
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

    # --- 세션 업데이트 ---
    if reps_count is not None:
        session.reps_count = reps_count
    if duration is not None:
        session.duration = duration

    try:
        session.full_clean()
    except ValidationError as e:
        return Response({"error": e.message_dict}, status=status.HTTP_400_BAD_REQUEST)

    session.save()

    # --- XP 계산 ---
    if reps_count is not None:
        user.xp += int(reps_count) * 10
    elif duration is not None:
        user.xp += int(duration.total_seconds()) * 2

    if session_duration_seconds is not None:
        try:
            user.total_time += float(session_duration_seconds)
        except ValueError:
            return Response({"error": "Invalid session_duration_seconds"}, status.HTTP_400_BAD_REQUEST)

        user.last_session_at = timezone.now()

    user.save()

    # --- 스케줄과 연결된 경우 ---
    if session.schedule:
        schedule = session.schedule

        if reps_count is not None:
            schedule.reps_done = reps_count
        if duration is not None:
            schedule.duration_done = duration

        # 상태 자동 계산
        if ((schedule.reps_target and schedule.reps_done >= schedule.reps_target) or
            (schedule.duration_target and schedule.duration_done >= schedule.duration_target)):
            schedule.status = 'completed'
        elif ((schedule.reps_done and schedule.reps_done > 0) or
              (schedule.duration_done and schedule.duration_done.total_seconds() > 0)):
            schedule.status = 'partial'

        schedule.save()

        feedback_text = generate_feedback_from_schedule(user, schedule)
        Feedback.objects.update_or_create(
            user=user,
            schedule=schedule,
            defaults={"summary_text": feedback_text}
        )

    serializer = SessionSerializer(session)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def sessions_view(request):
    user = request.user

    sessions = Session.objects.filter(user=user).order_by('-created_at')
    serializer = SessionSerializer(sessions, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

# -----------------------------------
# 🟢 Schedule CRUD
# -----------------------------------

@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def schedules_view(request):
    user = request.user

    if request.method == 'GET':
        status_filter = request.query_params.get('status')
        schedules = Schedule.objects.filter(user=user).order_by('-scheduled_date')
        if status_filter:
            schedules = schedules.filter(status=status_filter)
        serializer = ScheduleSerializer(schedules, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    elif request.method == 'POST':
        serializer = ScheduleSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(user=user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

# -----------------------------------
# 🟠 Schedule Edit
# -----------------------------------

@api_view(['PATCH', 'DELETE'])
@permission_classes([IsAuthenticated])
def schedule_edit(request, schedule_id):
    user = request.user
    schedule = get_object_or_404(Schedule, id=schedule_id, user=user)

    if request.method == 'PATCH':
        serializer = ScheduleSerializer(schedule, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    elif request.method == 'DELETE':
        schedule.delete()
        return Response({"message": "Schedule deleted successfully"}, status=status.HTTP_200_OK)

# 재시도 횟수 상수로 정의
MAX_RETRIES = 3 

# -----------------------------------
# 🟡 AI Auto-generate Schedules (고도화)
# -----------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def schedules_auto_generate(request):
    user = request.user
    
    # 사용자 설정 (요청 body에서 받거나 기본값 사용)
    # 삭제/편집 불가 모델이므로, 생성 개수를 보수적으로 설정
    num_schedules = min(int(request.data.get('num_schedules', 3)), 5) # 최대 5개로 보수적 조정
    target_date = request.data.get('target_date') # 특정 날짜 지정 가능
    
    recent_schedules = Schedule.objects.filter(user=user).order_by('-scheduled_date')[:5]
    history = []
    
    for s in recent_schedules:
        feedback_text = getattr(getattr(s, 'feedback', None), 'summary_text', "")
        history_item = {
            "scheduled_date": str(s.scheduled_date),
            "start_time": s.start_time.strftime("%H:%M"),
            "end_time": s.end_time.strftime("%H:%M"),
            "activity": s.activity,
            "status": s.status,
            "feedback": feedback_text
        }
        
        if s.reps_target is not None:
            history_item["reps_target"] = s.reps_target
            history_item["reps_done"] = s.reps_done or 0
        elif s.duration_target is not None:
            history_item["duration_target"] = int(s.duration_target.total_seconds())
            history_item["duration_done"] = int(s.duration_done.total_seconds()) if s.duration_done else 0
        
        history.append(history_item)
    
    initial_reps = getattr(user, 'initial_reps', 20)
    reps_based = [a.value for a, t in ACTIVITY_TYPE_MAP.items() if t == 'reps']
    duration_based = [a.value for a, t in ACTIVITY_TYPE_MAP.items() if t == 'duration']
    
    activity_rule = (
        f"Available activities:\n"
        f"- Reps-based: {', '.join(reps_based)} (use 'reps_target' field)\n"
        f"- Duration-based: {', '.join(duration_based)} (use 'duration_target' field in seconds)"
    )
    
    # ------------------
    # ✅ AI 프롬프트 생성 함수
    # ------------------
    def generate_prompt(history_data, num_sched, target_d):
        now_local = timezone.localtime() 
        now_str = now_local.isoformat()
        today_str = now_local.date().isoformat()
        logger.warning(f"오늘 날짜: {today_str}, 현재 시각: {now_str}")
        return f"""
        You are a professional fitness coach creating personalized workout schedules.

        User Info:
        - Baseline squat ability: {initial_reps} reps
        - Recent history: {json.dumps(history_data, indent=2) if history_data else "No history available"}

        Task:
        Generate {num_sched} realistic daily workout schedule(s) {f"for {target_d}" if target_d else f"starting from {now_str}"}.

        Rules:
        1. {activity_rule}
        2. Each schedule MUST have EITHER 'reps_target' OR 'duration_target' (never both)
        3. Consider user's fitness level and progress (Progressive overload: gradually increase difficulty if user is improving).
        4. Vary activities to avoid overtraining and boredom.
        5. **Schedule Time Flexibility**: Set the 'start_time' and 'end_time' to create a wide, flexible window (e.g., 60-90 minutes) around the estimated short actual workout time (e.g., 10-20 minutes). This gives the user flexibility to fit the workout into their life.
        6. **Time Distribution**: Distribute the schedules across different times of the day (morning, noon, evening) to cover various opportunities, but avoid unreasonable times (e.g., 03:00 AM).
        7. **Conservative Quantity**: Since the user cannot delete or edit schedules, generate only a conservative number ({num_sched} schedules) that is manageable to avoid excessive 'missed' statuses.
        8. The output format shown below is only an example of the required *structure*. You must follow the JSON format and key layout, but you should not copy or reuse the example values (such as times or numbers).
        9. All generated schedules must be set after the current time ({now_str}). 
           Do NOT generate schedules in the past (including earlier times on the same day). 
           If the current time has already passed common workout windows (e.g., evening), 
           generate schedules for the next valid day instead.

        Output Format (valid JSON only, no markdown):
        {{
            "schedules": [
                {{
                    "scheduled_date": "YYYY-MM-DD",
                    "start_time": "08:00",
                    "end_time": "09:00",
                    "activity": "squat",
                    "reps_target": 25
                }},
                {{
                    "scheduled_date": "YYYY-MM-DD",
                    "start_time": "18:30",
                    "end_time": "19:30",
                    "activity": "plank",
                    "duration_target": 60
                }}
            ]
        }}
        """
    # ------------------
    # ✅ AI 호출 및 재시도 로직
    # ------------------
    ai_output = None
    for attempt in range(MAX_RETRIES):
        prompt = generate_prompt(history, num_schedules, target_date)
        try:
            response = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "You are a professional fitness coach. Always respond with valid JSON only."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=1000,
            )
            ai_output = response.choices[0].message.content.strip()

            # JSON 파싱 및 마크다운 코드 블록 제거
            if ai_output.startswith("```"):
                ai_output = ai_output.split("```")[1]
                if ai_output.startswith("json"):
                    ai_output = ai_output[4:]
                ai_output = ai_output.strip()
            
            # 파싱 시도
            schedule_data = json.loads(ai_output)
            if not isinstance(schedule_data, dict) or "schedules" not in schedule_data:
                 raise ValueError("Invalid data structure")
            
            # 성공적으로 파싱되었으면 루프 탈출
            break 
            
        except (json.JSONDecodeError, ValueError) as e:
            logger.warning(f"AI returned invalid JSON on attempt {attempt + 1}: {ai_output[:200] if ai_output else 'No output'}. Retrying...")
            if attempt == MAX_RETRIES - 1:
                # 마지막 시도까지 실패
                logger.error(f"AI returned invalid JSON after {MAX_RETRIES} attempts.")
                return Response(
                    {"error": "AI returned invalid data format after multiple retries. Please try again."},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
        
        except OpenAIError as e:
            logger.error(f"OpenAI API error on attempt {attempt + 1} in auto-generate: {str(e)}")
            if attempt == MAX_RETRIES - 1:
                return Response(
                    {"error": "AI service temporarily unavailable. Please try again later."},
                    status=status.HTTP_503_SERVICE_UNAVAILABLE
                )
        except Exception as e:
            logger.error(f"Unexpected error on attempt {attempt + 1} in auto-generate: {str(e)}")
            if attempt == MAX_RETRIES - 1:
                return Response(
                    {"error": "An unexpected error occurred. Please try again."},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
    
    if not schedules_are_future(schedule_data["schedules"]):
        logger.warning("AI generated schedules in the past. Retrying...")
        raise ValueError("Generated schedules are in the past")

    # ------------------
    # ✅ 스케줄 저장 로직 (재시도 성공 후 실행)
    # ------------------
    created_schedules = []
    errors = []
    
    for idx, sched in enumerate(schedule_data.get("schedules", [])):
        activity_type = ACTIVITY_TYPE_MAP.get(sched.get("activity"))
    
        serializer_data = {
            "scheduled_date": sched.get("scheduled_date"),
            "start_time": sched.get("start_time"),
            "end_time": sched.get("end_time"),
            "activity": sched.get("activity"),
        }
    
        if activity_type == 'reps':
            serializer_data["reps_target"] = sched.get("reps_target")
        elif activity_type == 'duration':
            serializer_data["duration_target"] = sched.get("duration_target")
        
        serializer = ScheduleSerializer(data=serializer_data)
        
        if serializer.is_valid():
            serializer.save(user=user)
            created_schedules.append(serializer.data)
        else:
            errors.append({
                "index": idx,
                "data": sched,
                "errors": serializer.errors
            })
    
    response_data = {
        "message": f"Successfully created {len(created_schedules)} schedule(s)",
        "schedules": created_schedules
    }
    
    if errors:
        response_data["partial_errors"] = errors
        logger.warning(f"Some schedules failed validation: {errors}")
    
    return Response(response_data["schedules"], status=status.HTTP_201_CREATED)

def schedules_are_future(schedule_list):
    now = timezone.localtime()
    today = now.date()
    now_time = now.time()

    for s in schedule_list:
        s_date = datetime.strptime(s["scheduled_date"], "%Y-%m-%d").date()
        s_start = datetime.strptime(s["start_time"], "%H:%M").time()

        if s_date > today:
            continue
        
        if s_date == today and s_start < now_time:
            return False

    return True

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def mark_missed_schedules_view(request):
    try:
        mark_missed_schedules()
        return Response({"detail": "Missed schedules processed"}, status=status.HTTP_200_OK)
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


AUTO_GENERATE_DAYS = 4

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def auto_generate_schedules_view(request):
    user = request.user
    today = timezone.localdate()
    
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

    factory = APIRequestFactory()
    for date in missing_dates:
        req = factory.post(
            '/schedules/auto-generate/',
            data={"target_date": str(date), "num_schedules": 3},
            format='json'
        )
        force_authenticate(req, user=user)
        resp = schedules_auto_generate(req)
        if resp.status_code in [200, 201]:
            created_schedules.extend(resp.data)
        else:
            logger.warning(f"Failed to auto-generate schedules for {date}: {resp.data}")

    return Response(
        {
            "created_count": len(created_schedules),
            "dates_generated": [s['scheduled_date'] for s in created_schedules]
        },
        status=status.HTTP_201_CREATED
    )

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
    generate_daily_summaries_for_user(user)
    return Response({"message": "Daily summaries generated successfully."}, status=status.HTTP_200_OK)
