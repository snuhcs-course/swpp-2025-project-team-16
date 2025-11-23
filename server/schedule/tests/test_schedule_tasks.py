import pytest
from datetime import timedelta
from django.utils import timezone
from schedule.models import Schedule, Feedback
from schedule.tasks import mark_missed_schedules
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory
from unittest.mock import patch

@pytest.mark.django_db
def test_mark_missed_schedules_updates_status():
    user = UserFactory()
    schedule = ScheduleFactory(user=user, scheduled_date=timezone.localdate() - timedelta(days=1))
    schedule.status = "planned"
    schedule.save()
    with patch("schedule.utils.feedback.generate_feedback_from_schedule") as mock_feedback:
        mock_feedback.return_value = "Good work"
        mark_missed_schedules()
    schedule.refresh_from_db()
    assert schedule.status == "missed"
