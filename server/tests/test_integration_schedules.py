import pytest
from django.urls import reverse
from factories.schedule_factory import ScheduleFactory

@pytest.mark.django_db
def test_schedule_workflow(auth_client, user):
    # 스케줄 생성
    url = reverse("schedules_view")
    data = {"activity": "squat", "scheduled_date": "2025-01-01", "start_time": "08:00", "end_time": "09:00", "reps_target": 20}
    resp = auth_client.post(url, data, format="json")
    assert resp.status_code == 201

    # 세션 시작
    url_start = reverse("start_session")
    resp_start = auth_client.post(url_start, {"activity": "squat"}, format="json")
    assert resp_start.status_code == 201
