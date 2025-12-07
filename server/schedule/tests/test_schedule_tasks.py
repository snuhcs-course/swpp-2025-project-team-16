import pytest
from datetime import timedelta
from django.utils import timezone
from schedule.models import Schedule
from schedule.tasks import mark_missed_schedules
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory

@pytest.mark.django_db
def test_mark_missed_schedules_updates_status():
    user = UserFactory()
    schedule = ScheduleFactory(user=user, scheduled_date=timezone.localdate() - timedelta(days=1))
    schedule.status = "planned"
    schedule.save()
    mark_missed_schedules()
    schedule.refresh_from_db()
    assert schedule.status == "missed"
