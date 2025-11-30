import pytest
from unittest.mock import patch, MagicMock
from django.urls import reverse
from rest_framework import status
from pose.models import PoseAnalysis
from factories.user_factory import UserFactory
from factories.schedule_factory import SessionFactory, ScheduleFactory

@pytest.mark.django_db
def test_evaluate_posture_success(auth_client, user):
    session = SessionFactory(user=user)
    payload = {
        "session_id": session.id,
        "image_url": "http://example.com/img.png"
    }

    mock_result = {
        "pose_data": {"keypoints": []},
        "ai_comment": "Great posture!"
    }

    with patch("pose.views.run_pose_analysis") as mock_run:
        mock_run.return_value = (mock_result, 200)
        url = reverse("evaluate_posture")
        response = auth_client.post(url, payload, format="json")

    assert response.status_code == 201
    assert response.data["ai_comment"] == "Great posture!"
    assert response.data["session"] == session.id
    assert PoseAnalysis.objects.filter(user=user).exists()

@pytest.mark.django_db
def test_evaluate_posture_failure(auth_client):
    payload = {"session_id": 1}
    with patch("pose.views.run_pose_analysis") as mock_run:
        mock_run.return_value = ({"error": "Timeout"}, 504)
        url = reverse("evaluate_posture")
        response = auth_client.post(url, payload, format="json")

    assert response.status_code == 504
    assert "error" in response.data

@pytest.mark.django_db
def test_pose_analyses_view(auth_client, user):
    url = reverse("pose_analyses_view")
    response = auth_client.get(url)
    assert response.status_code == 200

@pytest.mark.django_db
def test_pose_analysis_detail(auth_client, user):
    pose = PoseAnalysis.objects.create(user=user)
    url = reverse("pose_analysis_detail", args=[pose.id])
    response = auth_client.get(url)
    assert response.status_code == 200
    assert response.data["id"] == pose.id

@pytest.mark.django_db
def test_pose_analyses_by_session(auth_client, user):
    session = SessionFactory(user=user)
    pose = PoseAnalysis.objects.create(user=user, session=session)
    url = reverse("pose_analyses_by_session", args=[session.id])
    response = auth_client.get(url)
    assert response.status_code == 200
    assert any(p["id"] == pose.id for p in response.data)

@pytest.mark.django_db
def test_pose_analyses_by_schedule(auth_client, user):
    schedule = ScheduleFactory(user=user)
    pose = PoseAnalysis.objects.create(user=user, schedule=schedule)
    url = reverse("pose_analyses_by_schedule", args=[schedule.id])
    response = auth_client.get(url)
    assert response.status_code == 200
    assert any(p["id"] == pose.id for p in response.data)
