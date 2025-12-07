import pytest
import base64
from unittest.mock import patch
from django.urls import reverse
from rest_framework import status

from pose.models import PoseAnalysis
from factories.schedule_factory import SessionFactory


# 임시 base64 이미지 생성
def fake_base64_image():
    return "data:image/jpeg;base64," + base64.b64encode(b"fake image content").decode("utf-8")


@pytest.mark.django_db
def test_evaluate_posture_success(auth_client, user, settings, tmp_path):

    # 테스트용 media 경로
    settings.MEDIA_ROOT = tmp_path
    settings.MEDIA_URL = "/media/"
    settings.ALLOWED_HOSTS = ["testserver"]

    session = SessionFactory(user=user)

    payload = {
        "image_base64": fake_base64_image(),
        "category": "squat",
        "schedule_id": session.schedule.id if session.schedule else None,
        "session_id": session.id,
    }

    mock_result = {
        "pose_data": {
            "joint_angles": {
                "left_knee": 95,
                "right_knee": 100
            },
            "keypoints_2d": [[1, 2], [3, 4], [5, 6]]
        },
        "summary": {
            "good_points": ["허리가 곧습니다."],
            "improvement_points": ["무릎 각도 개선 필요"],
            "improvement_methods": ["천천히 앉기"]
        }
    }

    with patch("pose.views.run_pose_analysis") as mock_run:
        mock_run.return_value = mock_result

        url = reverse("evaluate_posture")
        response = auth_client.post(url, payload, format="json")

    # ================= 검증 ==================
    assert response.status_code == status.HTTP_201_CREATED

    data = response.data

    assert data["good_points"] == ["허리가 곧습니다."]
    assert data["improvement_points"] == ["무릎 각도 개선 필요"]
    assert data["improvement_methods"] == ["천천히 앉기"]

    # DB 저장 확인
    assert PoseAnalysis.objects.count() == 1
    pose = PoseAnalysis.objects.first()

    assert pose.user == user
    assert pose.activity == "squat"
    assert pose.session_id == session.id
    assert pose.pose_data == mock_result["pose_data"]

    # 이미지 파일 실제 생성 확인
    assert (tmp_path / "pose_images").exists()
    assert len(list((tmp_path / "pose_images").iterdir())) == 1


@pytest.mark.django_db
def test_evaluate_posture_missing_fields(auth_client):
    url = reverse("evaluate_posture")

    payload = {"session_id": 1}
    response = auth_client.post(url, payload, format="json")

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.data["error"] == "image_base64 and category are required."


@pytest.mark.django_db
def test_evaluate_posture_analysis_failed(auth_client, user):
    session = SessionFactory(user=user)

    payload = {
        "image_base64": fake_base64_image(),
        "category": "squat",
        "session_id": session.id,
    }

    mock_result = {
        "error": "External evaluation failed."
    }

    with patch("pose.views.run_pose_analysis") as mock_run:
        mock_run.return_value = mock_result

        url = reverse("evaluate_posture")
        response = auth_client.post(url, payload, format="json")

    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
    assert "error" in response.data

    # DB 저장 안 됐는지 확인
    assert PoseAnalysis.objects.count() == 0
