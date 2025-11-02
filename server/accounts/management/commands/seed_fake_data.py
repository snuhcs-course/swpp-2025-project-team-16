from django.core.management.base import BaseCommand
from django.utils import timezone
from django.contrib.auth import get_user_model
from accounts.models import Account
from schedule.models import Sport, Session, Schedule, SportStatus
import random
from datetime import timedelta, time

User = get_user_model()


class Command(BaseCommand):
    help = "Seed fake users, sports, sessions, and schedules for testing"

    def handle(self, *args, **kwargs):
        self.stdout.write(self.style.WARNING("🚀 Starting fake data generation..."))

        # ====== 1️⃣ 유저 5명 생성 ======
        users = []
        for i in range(1, 6):
            email = f"test{i}@test.com"
            if not User.objects.filter(email=email).exists():
                user = User.objects.create_user(
                    email=email,
                    name=f"User {i}",
                    password="1234",
                    level=random.randint(1, 5),
                    total_time=random.randint(100, 1000),
                    initial_reps=random.randint(10, 30),
                )
                users.append(user)
                self.stdout.write(self.style.SUCCESS(f"✅ Created {email}"))
            else:
                users.append(User.objects.get(email=email))

        # ====== 2️⃣ 운동(Sport) 생성 ======
        sports_data = [
            ("Push-up", "Upper body exercise for chest & triceps", 10),
            ("Squat", "Lower body strengthening exercise", 8),
            ("Plank", "Core endurance and stability exercise", 5),
            ("Lunge", "Leg balance and strength exercise", 6),
            ("Burpee", "Full-body cardio exercise", 7),
        ]

        sports = []
        for name, desc, total_sessions in sports_data:
            sport, _ = Sport.objects.get_or_create(
                name=name,
                defaults={"description": desc, "total_sessions": total_sessions},
            )
            sports.append(sport)
        self.stdout.write(self.style.SUCCESS(f"✅ Created {len(sports)} sports"))

        # ====== 3️⃣ 세션(Session) 생성 ======
        sessions = []
        for sport in sports:
            for j in range(1, 4):
                s = Session.objects.create(
                    title=f"{sport.name} Level {j}",
                    description=f"Auto-generated session for {sport.name}",
                    sport=sport,
                    difficulty_level=random.choice(["Easy", "Medium", "Hard"]),
                    length=random.randint(10, 40),
                )
                sessions.append(s)
        self.stdout.write(self.style.SUCCESS(f"✅ Created {len(sessions)} sessions"))

        # ====== 4️⃣ 스케줄(Schedule) 생성 ======
        today = timezone.now().date()
        total_days = 10  # 각 유저당 10일치 스케줄

        for user in users:
            for i in range(total_days):
                # ✅ -5 ~ +5일 사이의 랜덤 날짜 (즉, 과거 + 오늘 + 미래)
                date_offset = random.randint(-5, 5)
                schedule_date = today + timedelta(days=date_offset)

                start = time(hour=random.randint(6, 18), minute=0)
                end_hour = min(start.hour + random.randint(1, 2), 23)
                end = time(hour=end_hour, minute=0)

                session = random.choice(sessions)

                Schedule.objects.create(
                    user=user,
                    session=session,
                    date=schedule_date,
                    start_time=start,
                    end_time=end,
                    name=f"{session.title} Routine",
                    is_finished=schedule_date < today,  # 과거는 완료됨 / 미래는 미완료
                )

        self.stdout.write(self.style.SUCCESS("✅ Created schedules for all users (past + today + future)"))

        # ====== 5️⃣ 운동 상태(SportStatus) 생성 ======
        for user in users:
            for sport in sports:
                SportStatus.objects.get_or_create(
                    user=user,
                    sport=sport,
                    session=random.choice(sessions),
                    proficiency_level=random.choice(["Beginner", "Intermediate", "Advanced"]),
                    last_practiced=today - timedelta(days=random.randint(0, 7)),
                )

        self.stdout.write(self.style.SUCCESS("✅ Created sport statuses"))
        self.stdout.write(self.style.SUCCESS("🎉 Fake data generation complete!"))
