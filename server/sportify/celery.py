import os
from celery import Celery
from celery.schedules import crontab  # Beat 스케줄 사용

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "sportify.settings")

app = Celery("sportify")
app.config_from_object("django.conf:settings", namespace="CELERY")

# 앱에서 tasks.py 자동 발견
app.autodiscover_tasks()

# 주기적 실행 스케줄 (Beat)
app.conf.beat_schedule = {
    "mark-missed-every-10-min": {
        "task": "schedule.celery_tasks.run_mark_missed_schedules",
        "schedule": crontab(minute="*/10"),  # 10분마다 실행
    },
}

