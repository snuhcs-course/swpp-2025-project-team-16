from rest_framework.decorators import api_view, authentication_classes, permission_classes
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.authentication import JWTAuthentication
from django.contrib.auth import authenticate
from django.conf import settings

from .models import Account
from .serializers import AccountSerializer
from rest_framework.permissions import AllowAny

SECRET_KEY = settings.SECRET_KEY

# ✅ 이메일 존재 여부 확인
@api_view(['POST'])
@permission_classes([AllowAny])
@authentication_classes([])
def check_email(request):
    """
    Body: { "email": "user@example.com" }
    Response: { "exists": true/false }
    """
    email = request.data.get("email")
    exists = Account.objects.filter(email=email).exists()
    return Response({"exists": exists}, status=status.HTTP_200_OK)


# ✅ 회원가입 (회원 생성 + AccessToken 즉시 발급)
@api_view(['POST'])
@permission_classes([AllowAny])
@authentication_classes([])
def signup(request):
    """
    Body: { "name": "...", "email": "...", "password": "..." }
    Response: { "message": "...", "token": "..." }
    """
    serializer = AccountSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.save()  # 내부에서 create_user() 호출 → 비밀번호 자동 해싱됨

        # JWT 발급 (SimpleJWT)
        refresh = RefreshToken.for_user(user)
        access_token = str(refresh.access_token)

        return Response({
            "message": "Signup success!",
            "token": access_token
        }, status=status.HTTP_201_CREATED)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


# ✅ 로그인 (Django 인증 + SimpleJWT 토큰 발급)
@api_view(['POST'])
@permission_classes([AllowAny])
@authentication_classes([])
def login(request):
    """
    Body: { "email": "...", "password": "..." }
    Response: { "token": "...", "name": "...", "level": int, "xp": int }
    """
    email = request.data.get('email')
    password = request.data.get('password')

    user = authenticate(request, email=email, password=password)
    if user is None:
        return Response({"error": "Invalid email or password"}, status=status.HTTP_400_BAD_REQUEST)

    # SimpleJWT 토큰 발급
    refresh = RefreshToken.for_user(user)
    access_token = str(refresh.access_token)

    return Response({
        "token": access_token,
        "name": user.name,
        "level": user.level,
        "xp": user.xp
    }, status=status.HTTP_200_OK)


# ✅ 초기 운동 개수 업데이트 (JWT 인증 필요)
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def update_initial_reps(request):
    """
    Header: Authorization: Bearer <JWT>
    Body: { "initial_reps": 15 }
    """
    user = request.user  # ✅ SimpleJWT 인증을 통해 자동 주입됨
    initial_reps = int(request.data.get("initial_reps", 0))
    user.initial_reps = initial_reps
    user.save()

    return Response({
        "message": "Initial reps saved successfully",
        "initial_reps": user.initial_reps
    }, status=status.HTTP_200_OK)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_rankings(request):
    """
    GET /accounts/rankings/
    공동 순위 반영 버전
    """
    users = Account.objects.filter(is_active=True).order_by('-xp', '-level')[:50]

    data = []
    prev_xp = None
    rank = 0
    display_rank = 0

    for u in users:
        display_rank += 1
        if u.xp != prev_xp:
            rank = display_rank 
        data.append({
            "rank": rank,
            "name": u.name,
            "xp": u.xp,
            "level": u.level,
        })
        prev_xp = u.xp

    return Response(data, status=status.HTTP_200_OK)

