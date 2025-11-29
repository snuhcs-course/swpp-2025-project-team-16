from datetime import datetime, timedelta
from django.utils import timezone
from django.db import transaction
from schedule.models import Schedule
from django.utils.timezone import make_aware
from django.utils.timezone import localdate


def get_schedule_datetime(schedule):
    """Schedule의 scheduled_date와 start_time, end_time을 datetime으로 반환"""
    start_dt = make_aware(datetime.combine(schedule.scheduled_date, schedule.start_time))
    end_dt = make_aware(datetime.combine(schedule.scheduled_date, schedule.end_time))
    return start_dt, end_dt


@transaction.atomic
def mark_missed_schedules():
    now = timezone.now()
    planned_schedules = Schedule.objects.filter(status='planned')

    for s in planned_schedules:
        _, end_dt = get_schedule_datetime(s)

        if now > end_dt:
            s.status = 'missed'

            if s.reps_target:
                s.reps_done = 0
            elif s.duration_target:
                s.duration_done = timedelta(seconds=0)

            s.save()
