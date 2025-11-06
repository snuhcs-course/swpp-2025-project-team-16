from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.shortcuts import get_object_or_404
from django.utils.dateparse import parse_date, parse_time
from .models import Schedule, Session
from accounts.models import Account
from .serializers import ScheduleSerializer
import jwt
from django.db.models import Sum

SECRET_KEY = "django-insecure-($$s2w-4hgos)68o7$h$6twbwamtm56)%24e4ggj4=*rvrfv#1"

def get_user_from_token(request):
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return None
    token = auth_header.split(' ')[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=['HS256'])
        return Account.objects.get(id=payload['id'])
    except Exception:
        return None

@api_view(['GET', 'POST', 'DELETE'])
def schedule_list(request):
    user = get_user_from_token(request)
    if not user:
        return Response({'error': 'Unauthorized'}, status=status.HTTP_401_UNAUTHORIZED)

    # 🟢 GET: Get all schedules of the current user
    if request.method == 'GET':
        schedules = Schedule.objects.filter(user=user)
        serializer = ScheduleSerializer(schedules, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    # 🟠 POST: Create a new schedule
    elif request.method == 'POST':
        try:
            session_id = request.data.get('sessionId')
            date = parse_date(request.data.get('date'))
            start_time = parse_time(request.data.get('startTime'))
            end_time = parse_time(request.data.get('endTime'))

            session = get_object_or_404(Session, id=session_id)
            schedule = Schedule.objects.create(
                user=user,
                session=session,
                date=date,
                start_time=start_time,
                end_time=end_time,
                name=session.title,
                is_finished=False,
            )
            schedule.save()
            return Response({'message': 'Schedule created successfully'}, status=status.HTTP_201_CREATED)
        except Exception as e:
            return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

    # 🔴 DELETE: delete schedule by id
    elif request.method == 'DELETE':
        schedule_id = request.query_params.get('id')
        schedule = get_object_or_404(Schedule, id=schedule_id, user=user)
        schedule.delete()
        return Response({'message': 'Schedule deleted successfully'}, status=status.HTTP_200_OK)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_schedule(request):
    """
    GET /api/schedules/
    Response:
    [
      {
        "date": "2025-11-01",
        "xp": 250,
        "exercises": [
          {"name": "Push-up", "detail": "15 reps × 3 sets", "status": "Ready"}
        ]
      }
    ]
    """
    schedules = Schedule.objects.filter(user=request.user).select_related('session__sport').order_by('-date')
    serializer = ScheduleSerializer(schedules, many=True)

    grouped = {}
    for item in serializer.data:
        date = item["date"]
        if date not in grouped:
            grouped[date] = {"date": date, "xp": 0, "exercises": []}
        grouped[date]["exercises"].append({
            "name": item["name"],
            "detail": f"{item['session']} | {item['start_time']}–{item['end_time']}",
            "status": "Completed" if item["is_finished"] else "Ready"
        })
        grouped[date]["xp"] += 100  # ⚙️ 단순 가중치 (원하면 나중에 계산 로직 연결)

    result = list(grouped.values())
    return Response(result)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_history(request):
    """
    GET /api/accounts/history/
    사용자 운동 기록 반환
    """
    schedules = Schedule.objects.filter(user=request.user).order_by('-date')
    serializer = ScheduleSerializer(schedules, many=True)
    return Response(serializer.data)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def generate_schedule(request):
    """
    POST /schedule/generate_schedule
    Body (ScheduleResponse):
    {
        "id": "1",
        "date": "2025-11-06",
        "exercises": [
            {"name": "스쿼트", "repTarget": 10, "duration": "30초"}
        ],
        "startTime": "08:30:00",
        "finishTime": "09:30:00",
        "point": 30,
        "isCompleted": false,
        "feedback": ""
    }
    """
    # user = get_user_from_token(request)
    # if not user:
    #     return Response({'error': 'Unauthorized'}, status=status.HTTP_401_UNAUTHORIZED)

    user = request.user

    try:
        # ✅ 1️⃣ 요청 파싱
        date_str = request.data.get('date')
        start_time_str = request.data.get('startTime')
        finish_time_str = request.data.get('finishTime')

        existing = Schedule.objects.filter(user=user, date=parsed_date).first()
        if existing:
            return Response({'message': f'Schedule for {date_str} already exists'}, status=status.HTTP_200_OK)

        # ✅ 2️⃣ 날짜·시간 파싱
        parsed_date = parse_date(date_str)
        start_time = parse_time(start_time_str)
        finish_time = parse_time(finish_time_str)

        # ✅ 3️⃣ Schedule 생성
        schedule = Schedule.objects.create(
            user=user,
            date=parsed_date,
            start_time=start_time,
            end_time=finish_time,
            name=f"Workout Plan {date_str}",
            is_finished=False,
        )

        return Response(
            {"message": "Schedule created successfully", "schedule_id": schedule.id},
            status=status.HTTP_201_CREATED
        )

    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_stats(request):
    """
    GET /profile/stats
    Response:
    {
      "rank": 1,
      "level": 25,
      "total_time": "3h 42m",
      "xp": 1240
    }
    """
    # user = get_user_from_token(request)
    # if not user:
    #     return Response({'error': 'Unauthorized'}, status=status.HTTP_401_UNAUTHORIZED)
    user = request.user

    try:
        # 🔹 XP 계산 (모든 스케줄의 포인트 합)
        total_xp = Schedule.objects.filter(user=user).aggregate(Sum('point'))['point__sum'] or 0

        # 🔹 총 운동 시간 계산
        schedules = Schedule.objects.filter(user=user).exclude(start_time=None, end_time=None)
        total_minutes = 0
        for s in schedules:
            try:
                delta = (s.end_time.hour * 60 + s.end_time.minute) - (s.start_time.hour * 60 + s.start_time.minute)
                if delta > 0:
                    total_minutes += delta
            except Exception:
                pass
        total_time = f"{total_minutes // 60}h {total_minutes % 60}m"

        # 🔹 단순한 랭킹 계산 (XP 기준)
        all_users = (
            Schedule.objects.values('user')
            .annotate(total_xp=Sum('point'))
            .order_by('-total_xp')
        )
        rank = next((i + 1 for i, u in enumerate(all_users) if u['user'] == user.id), None)
        rank = rank or len(all_users)

        # 🔹 레벨 계산 (예: XP 100당 1레벨)
        level = total_xp // 100

        data = {
            'rank': rank,
            'level': level,
            'total_time': total_time,
            'xp': total_xp
        }

        return Response(data, status=status.HTTP_200_OK)

    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)