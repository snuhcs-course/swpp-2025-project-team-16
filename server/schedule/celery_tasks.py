from celery import shared_task
from .tasks import mark_missed_schedules

@shared_task
def run_mark_missed_schedules():
    mark_missed_schedules()
