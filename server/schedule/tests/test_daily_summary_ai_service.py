import pytest
from unittest.mock import patch, MagicMock
from datetime import timedelta, date, datetime
from schedule.services.daily_summary_ai_service import (
    generate_daily_summary,
    extract_target_done,
    DailySummaryError,
    APIResponseError,
)
from schedule.models import Schedule, Session, DailySummary
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory, SessionFactory
from openai import OpenAIError

@pytest.mark.django_db
def test_extract_target_done_reps():
    sched = ScheduleFactory(activity="squat", reps_target=20, reps_done=10)
    unit, target, done = extract_target_done(sched)

    assert unit == "reps"
    assert target == 20
    assert done == 10

@pytest.mark.django_db
def test_extract_target_done_duration():
    sched = ScheduleFactory(
        activity="plank",
        duration_target=timedelta(seconds=60),
        duration_done=timedelta(seconds=40),
    )
    unit, target, done = extract_target_done(sched)

    assert unit == "seconds"
    assert target == 60
    assert done == 40


@pytest.mark.django_db
def test_generate_daily_summary_success():
    user = UserFactory()
    day = date.today()

    with patch("schedule.services.daily_summary_ai_service.client.chat.completions.create") as mock_ai:
        mock_ai.return_value = MagicMock(
            choices=[MagicMock(
                message=MagicMock(content="🔥 Great effort today!")
            )]
        )

        summary = generate_daily_summary(user, day)

    assert "effort" in summary


@pytest.mark.django_db
def test_generate_daily_summary_api_error():
    user = UserFactory()
    day = date.today()

    with patch("schedule.services.daily_summary_ai_service.client.chat.completions.create") as mock_ai:
        mock_ai.side_effect = OpenAIError("API error")

        with pytest.raises(DailySummaryError):
            generate_daily_summary(user, day)


@pytest.mark.django_db
def test_generate_daily_summary_invalid_response():
    user = UserFactory()
    day = date.today()

    with patch("schedule.services.daily_summary_ai_service.client.chat.completions.create") as mock_ai:
        mock_ai.return_value = MagicMock(choices=[])

        with pytest.raises(APIResponseError):
            generate_daily_summary(user, day)
