from django.shortcuts import render
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.hashers import check_password, make_password
from .models import Account
from .serializers import AccountSerializer
import jwt, datetime, json

SECRET_KEY = 'sportify_secret_key'  # ⚠️ 나중에 .env로 분리 권장


# ✅ 이메일 존재 여부 확인 (LoginEmailFragment와 통신)
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
    serializer = AccountSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save()
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

    try:
        user = Account.objects.get(email=email)
    except Account.DoesNotExist:
        return Response({"error": "Invalid email or password"}, status=status.HTTP_400_BAD_REQUEST)

    if not check_password(password, user.password):
        return Response({"error": "Invalid email or password"}, status=status.HTTP_400_BAD_REQUEST)

    payload = {
        'id': user.id,
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=6),
        'iat': datetime.datetime.utcnow()
    }
    token = jwt.encode(payload, SECRET_KEY, algorithm='HS256')
    return Response({"token": token, "name": user.name}, status=status.HTTP_200_OK)
