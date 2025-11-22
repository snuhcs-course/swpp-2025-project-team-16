from datetime import datetime, timedelta
from django.utils import timezone
from django.db import transaction
from schedule.models import Schedule, Feedback
from schedule.utils.feedback import generate_feedback_from_schedule
from django.utils.timezone import make_aware


def get_schedule_datetime(schedule):
    """Schedule의 scheduled_date와 start_time, end_time을 datetime으로 반환"""
    start_dt = make_aware(datetime.combine(schedule.scheduled_date, schedule.start_time))
    end_dt = make_aware(datetime.combine(schedule.scheduled_date, schedule.end_time))
    return start_dt, end_dt


@transaction.atomic
def mark_missed_schedules():
    """
    완료되지 않은 지난 스케줄을 'missed' 상태로 업데이트하고
    reps_done/duration_done은 0으로 설정.
    이후 Feedback을 자동 생성.
    """
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

            # feedback_exists = Feedback.objects.filter(user=s.user, schedule=s).exists()
            # if not feedback_exists:
            #     feedback_text = generate_feedback_from_schedule(s.user, s)
            #     Feedback.objects.create(
            #         user=s.user,
            #         schedule=s,
            #         summary_text=feedback_text
            #     )
