from django.db import models
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin


# ✅ 사용자 생성 전용 매니저 (변경 없음)
class AccountManager(BaseUserManager):
    def create_user(self, email, name, password=None, **extra_fields):
        if not email:
            raise ValueError("Users must have an email address")
        email = self.normalize_email(email)
        user = self.model(email=email, name=name, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, name, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, name, password, **extra_fields)


# ✅ 실제 사용자 모델
class Account(AbstractBaseUser, PermissionsMixin):
    email = models.EmailField(unique=True)
    name = models.CharField(max_length=100)

    # --- 운동/활동 관련 필드 확장 ---
    xp = models.IntegerField(default=0)                  # 누적 경험치
    level = models.IntegerField(default=1)               # ✅ 사용자 레벨
    initial_reps = models.IntegerField(default=0)        # 최초 설정 운동 개수
    total_reps = models.IntegerField(default=0)          # 누적 운동 횟수
    total_time = models.FloatField(default=0.0)          # ✅ 누적 운동 시간 (단위: 초 or 분)
    last_session_at = models.DateTimeField(null=True, blank=True)  # 마지막 세션 시각

    # --- 기본 Django 사용자 필드 ---
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    date_joined = models.DateTimeField(auto_now_add=True)

    objects = AccountManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name']

    class Meta:
        db_table = 'accounts'

    def __str__(self):
        return self.email
