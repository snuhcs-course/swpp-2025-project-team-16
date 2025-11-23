import pytest
from schedule.serializers import ScheduleSerializer
from datetime import timedelta
from factories.schedule_factory import ScheduleFactory

@pytest.mark.django_db
def test_schedule_validation_reps():
    data = {
        "scheduled_date": "2025-01-01",
        "start_time": "10:00",
        "end_time": "11:00",
        "activity": "squat",  # reps 기반
        "duration_target": 60  # duration 들어오면 에러
    }

    serializer = ScheduleSerializer(data=data)
    assert serializer.is_valid() is False
    assert "non_field_errors" in serializer.errors

@pytest.mark.django_db
def test_schedule_creation_defaults():
    schedule = ScheduleFactory()
    assert schedule.status == "planned"
    assert schedule.reps_done == 0 or schedule.reps_done is None
    assert schedule.duration_done == timedelta(seconds=0) or schedule.duration_done is None
