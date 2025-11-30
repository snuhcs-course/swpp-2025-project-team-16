import pytest
import json
from unittest.mock import patch, MagicMock
from datetime import date, timedelta
from schedule.services.schedule_ai_service import (
    generate_daily_schedules,
    AIScheduleError,
    generate_activity_rule,
    generate_scheduling_prompt,
)
from schedule.models import Schedule, ActivityType
from factories.schedule_factory import ScheduleFactory
from factories.user_factory import UserFactory
from openai import OpenAIError

@pytest.mark.django_db
def test_generate_daily_schedules_success():
    user = UserFactory()
    today = date.today()

    with patch("schedule.services.schedule_ai_service.client.chat.completions.create") as mock_openai:
        mock_openai.return_value = MagicMock(
            choices=[MagicMock(
                message=MagicMock(
                    content='{"schedules":[{"scheduled_date":"2030-01-01","start_time":"08:00","end_time":"09:00","activity":"squat","reps_target":20}]}'
                )
            )]
        )

        result = generate_daily_schedules(user, today)

    assert "schedules" in result
    assert result["schedules"][0]["activity"] == "squat"


@pytest.mark.django_db
def test_generate_daily_schedules_invalid_json():
    user = UserFactory()

    with patch("schedule.services.schedule_ai_service.client.chat.completions.create") as mock_openai:
        mock_openai.return_value = MagicMock(
            choices=[MagicMock(
                message=MagicMock(content="{invalid json")
            )]
        )

        with pytest.raises(AIScheduleError):
            generate_daily_schedules(user, date.today())


@pytest.mark.django_db
def test_generate_daily_schedules_openai_error():
    user = UserFactory()

    with patch("schedule.services.schedule_ai_service.client.chat.completions.create") as mock_openai:
        mock_openai.side_effect = OpenAIError("API failed")

        with pytest.raises(AIScheduleError):
            generate_daily_schedules(user, date.today())

@pytest.mark.django_db
def test_generate_activity_rule():
    activity_map = {
        ActivityType.SQUAT: "reps",
        ActivityType.PLANK: "duration"
    }

    rule = generate_activity_rule(activity_map)
    assert "Reps-based" in rule
    assert "Duration-based" in rule

@pytest.mark.django_db
def test_generate_scheduling_prompt():
    prompt = generate_scheduling_prompt(
        date=date(2030, 1, 1),
        initial_reps=20,
        activity_rule="rule info",
        history_data=[]
    )

    assert "2030-01-01" in prompt
    assert "rule info" in prompt
    assert "Baseline squat ability" in prompt
