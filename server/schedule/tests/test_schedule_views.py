import pytest
from unittest.mock import patch, MagicMock
from django.urls import reverse
from schedule.models import Schedule, Session, DailySummary
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory, SessionFactory
from datetime import date

@pytest.mark.django_db
def test_schedules_view_get(auth_client, user):
    ScheduleFactory(user=user)
    url = reverse("schedules_view")
    response = auth_client.get(url)
    assert response.status_code == 200
    assert len(response.data) == 1

@pytest.mark.django_db
def test_schedules_auto_generate(auth_client):
    url = reverse("schedules_auto_generate")
    
    with patch("schedule.views.client.chat.completions.create") as mock_openai:
        mock_openai.return_value = MagicMock(
            choices=[MagicMock(message=MagicMock(content='{"schedules":[{"scheduled_date":"2025-01-01","start_time":"08:00","end_time":"09:00","activity":"squat","reps_target":20}]}'))]
        )
        response = auth_client.post(url, format="json")
    
    assert response.status_code == 201
    assert "created_count" in response.data

@pytest.mark.django_db
def test_mark_missed_schedules_view(auth_client):
    url = reverse("mark_missed_schedules_view")
    res = auth_client.post(url)
    assert res.status_code == 200
    assert res.data["detail"] == "Missed schedules processed"

@pytest.mark.django_db
def test_sessions_view(auth_client, user):
    SessionFactory(user=user)
    url = reverse("sessions_view")
    res = auth_client.get(url)
    assert res.status_code == 200
    assert len(res.data) == 1

@pytest.mark.django_db
def test_start_session(auth_client):
    url = reverse("start_session")
    res = auth_client.post(url, {"activity": "squat"}, format="json")

    assert res.status_code == 201
    assert res.data["activity"] == "squat"

@pytest.mark.django_db
def test_end_session_increases_xp(auth_client, user):
    start_res = auth_client.post(reverse("start_session"), {"activity": "squat"}, format="json")
    session_id = start_res.data["id"]

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
    response = auth_client.patch(url, data, format="json")
    assert response.status_code == 200
    assert response.data["reps_count"] == 15

@pytest.mark.django_db
def test_schedule_status_auto_complete(auth_client, user):
    schedule = ScheduleFactory(
        user=user,
        reps_target=10,
        activity="squat"
    )
    
    start_res = auth_client.post(
        reverse("start_session"),
        {"activity": "squat", "schedule_id": schedule.id},
        format="json"
    )
    session_id = start_res.data["id"]

    auth_client.patch(
        reverse("end_session", args=[session_id]),
        {"reps_count": 10},
        format="json"
    )

    schedule.refresh_from_db()
    assert schedule.status == "completed"

@pytest.mark.django_db
def test_daily_summaries_view(auth_client, user):
    DailySummary.objects.create(user=user, date=date(2030,1,1), summary_text="test")
    url = reverse("daily_summaries_view")
    res = auth_client.get(url)
    assert res.status_code == 200
    assert len(res.data) == 1

@pytest.mark.django_db
def test_daily_summaries_auto_generate(auth_client, user):
    url = reverse("daily_summaries_auto_generate")

    with patch("schedule.views.generate_daily_summaries_for_user") as mock_service:
        mock_service.return_value = [{"date": date(2030,1,1), "summary": "good"}]

        res = auth_client.post(url)

    assert res.status_code == 201
    assert res.data["message"] == "Daily summaries generated successfully."
