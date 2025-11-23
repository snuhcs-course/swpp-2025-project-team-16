import pytest
from unittest.mock import patch, MagicMock
from django.urls import reverse
from schedule.models import Schedule, Session
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory, SessionFactory

@pytest.mark.django_db
def test_schedules_view_get(auth_client, user):
    ScheduleFactory(user=user)
    url = reverse("schedules_view")
    response = auth_client.get(url)
    assert response.status_code == 200
    assert len(response.data) == 1

@pytest.mark.django_db
def test_create_schedule(auth_client):
    url = reverse("schedules_view")
    data = {
        "scheduled_date": "2025-01-01",
        "start_time": "10:00",
        "end_time": "11:00",
        "activity": "squat",
        "reps_target": 30
    }

    res = auth_client.post(url, data, format="json")
    assert res.status_code == 201
    assert res.data["reps_target"] == 30

@pytest.mark.django_db
def test_start_session(auth_client):
    url = reverse("start_session")
    res = auth_client.post(url, {"activity": "squat"}, format="json")

    assert res.status_code == 201
    assert res.data["activity"] == "squat"

@pytest.mark.django_db
def test_end_session_increases_xp(auth_client, user):
    # 먼저 세션 생성
    start_res = auth_client.post(reverse("start_session"), {"activity": "squat"}, format="json")
    session_id = start_res.data["id"]

    # 종료 시 reps_count = 5 → XP = 5 * 10 = 50 증가
    end_res = auth_client.patch(
        reverse("end_session", args=[session_id]),
        {"reps_count": 5},
        format="json"
    )

    user.refresh_from_db()
    assert user.xp == 50

@pytest.mark.django_db
def test_end_session_with_reps(auth_client, user):
    session = SessionFactory(user=user, reps_count=None, duration=None)
    url = reverse("end_session", args=[session.id])
    data = {"reps_count": 15}
    
    with patch("schedule.utils.feedback.client.chat.completions.create") as mock_openai:
        mock_openai.return_value = MagicMock(
            choices=[MagicMock(message=MagicMock(content="🎉 Great job!"))]
        )
        response = auth_client.patch(url, data, format="json")
    
    assert response.status_code == 200
    assert response.data["reps_count"] == 15

@pytest.mark.django_db
def test_schedule_status_auto_complete(auth_client, user):
    # 스케줄 생성
    sched_res = auth_client.post(
        reverse("schedules_view"),
        {
            "scheduled_date": "2025-01-01",
            "start_time": "10:00",
            "end_time": "11:00",
            "activity": "squat",
            "reps_target": 10
        },
        format="json"
    )

    schedule_id = sched_res.data["id"]

    # 세션 시작
    start_res = auth_client.post(
        reverse("start_session"), 
        {"activity": "squat", "schedule_id": schedule_id},
        format="json"
    )
    session_id = start_res.data["id"]

    with patch("schedule.utils.feedback.generate_feedback_from_schedule") as mock_feedback:
        mock_feedback.return_value = "Dummy feedback"

        # 종료 시 reps_done = reps_target
        auth_client.patch(
            reverse("end_session", args=[session_id]),
            {"reps_count": 10},
            format="json"
        )

    sched = Schedule.objects.get(id=schedule_id)
    assert sched.status == "completed"

@pytest.mark.django_db
def test_schedules_auto_generate_view(auth_client):
    url = reverse("auto_generate_schedules_view")
    
    with patch("schedule.views.client.chat.completions.create") as mock_openai:
        mock_openai.return_value = MagicMock(
            choices=[MagicMock(message=MagicMock(content='{"schedules":[{"scheduled_date":"2025-01-01","start_time":"08:00","end_time":"09:00","activity":"squat","reps_target":20}]}'))]
        )
        response = auth_client.post(url, format="json")
    
    assert response.status_code == 201
    assert "created_count" in response.data