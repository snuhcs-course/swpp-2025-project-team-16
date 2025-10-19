from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from django.shortcuts import get_object_or_404
from django.utils.dateparse import parse_date, parse_time
from .models import Schedule, Session
from accounts.models import Account
from .serializers import ScheduleSerializer
import jwt

SECRET_KEY = 'sportify_secret_key'


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
            return Response({'message': 'Schedule created successfully'}, status=status.HTTP_201_CREATED)
        except Exception as e:
            return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

    # 🔴 DELETE: delete schedule by id
    elif request.method == 'DELETE':
        schedule_id = request.query_params.get('id')
        schedule = get_object_or_404(Schedule, id=schedule_id, user=user)
        schedule.delete()
        return Response({'message': 'Schedule deleted successfully'}, status=status.HTTP_200_OK)
