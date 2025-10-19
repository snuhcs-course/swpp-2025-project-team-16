from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth import authenticate
from .models import Account
from .serializers import AccountSerializer
import jwt, datetime

SECRET_KEY = 'sportify_secret_key'  # ⚠️ TODO: .env로 옮기기


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
    Response: { "token": "...", "name": "..." }
    """
    email = request.data.get('email')
    password = request.data.get('password')

    # 🚀 Django의 인증 시스템을 직접 사용 (check_password 대체)
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
        "name": user.name
    }, status=status.HTTP_200_OK)
