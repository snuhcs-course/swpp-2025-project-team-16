from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth import authenticate
from rest_framework.permissions import IsAuthenticated
from .models import Account
from .serializers import AccountSerializer
import jwt, datetime

from django.conf import settings
from django.utils import timezone

# ⚠️ 실제 배포 시 .env / settings.SECRET_KEY 로 옮길 것
SECRET_KEY = getattr(settings, "SPORTIFY_SECRET_KEY", "sportify_secret_key")


# ✅ 이메일 존재 여부 확인 (LoginEmailFragment → 백엔드)
@api_view(['POST'])
def check_email(request):
    """
    Body: { "email": "user@example.com" }
    Response: { "exists": true/false }
    """
    email = request.data.get("email")
    exists = Account.objects.filter(email=email).exists()
    return Response({"exists": exists}, status=status.HTTP_200_OK)


# ✅ 회원가입 API
@api_view(['POST'])
def signup(request):
    """
    Body: { "name": "...", "email": "...", "password": "..." }
    """
    serializer = AccountSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save()  # create_user() 내부에서 비밀번호 자동 해싱
        return Response({"message": "Signup success"}, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


# ✅ 로그인 API
@api_view(['POST'])
def login(request):
    """
    Body: { "email": "...", "password": "..." }
    Response: { "token": "...", "name": "...", "level": int }
    """
    email = request.data.get('email')
    password = request.data.get('password')

    # Django 인증 사용
    user = authenticate(request, email=email, password=password)
    if user is None:
        return Response({"error": "Invalid email or password"}, status=status.HTTP_400_BAD_REQUEST)

    payload = {
        'id': user.id,
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=6),
        'iat': datetime.datetime.utcnow()
    }
    token = jwt.encode(payload, SECRET_KEY, algorithm='HS256')

    return Response({
        "token": token,
        "name": user.name,
        "level": user.level,
        "xp": user.xp
    }, status=status.HTTP_200_OK)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def update_initial_reps(request):
    """
    Header: Authorization: Bearer <JWT>
    Body: { "initial_reps": 15 }
    """
    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        return Response({"error": "Missing token"}, status=status.HTTP_401_UNAUTHORIZED)

    token = auth_header.split(" ")[1]
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
        user = Account.objects.get(id=payload["id"])
    except (jwt.ExpiredSignatureError, jwt.DecodeError, Account.DoesNotExist):
        return Response({"error": "Invalid or expired token"}, status=status.HTTP_401_UNAUTHORIZED)

    initial_reps = int(request.data.get("initial_reps", 0))
    user.initial_reps = initial_reps
    user.save()

    return Response({
        "message": "Initial reps saved successfully",
        "initial_reps": user.initial_reps
    }, status=status.HTTP_200_OK)
