import pytest
from django.urls import reverse
from accounts.models import Account

@pytest.mark.django_db
def test_check_email_exists(api_client, user):
    url = reverse('check_email')
    res = api_client.post(url, {"email": user.email})
    assert res.status_code == 200
    assert res.data["exists"] is True

@pytest.mark.django_db
def test_check_email_not_exists(api_client):
    url = reverse('check_email')
    res = api_client.post(url, {"email": "nouser@example.com"})
    assert res.status_code == 200
    assert res.data["exists"] is False

@pytest.mark.django_db
def test_signup(api_client):
    url = reverse("signup")
    data = {
        "name": "New",
        "email": "new@a.com",
        "password": "abcd"
    }

    res = api_client.post(url, data, format="json")
    assert res.status_code == 201
    assert "token" in res.data

@pytest.mark.django_db
def test_login_success(api_client, user):
    url = reverse("login")
    response = api_client.post(
        url, {"email": user.email, "password": "1234"}, format="json"
    )
    assert response.status_code == 200
    assert "token" in response.data

@pytest.mark.django_db
def test_login_fail(api_client):
    url = reverse("login")
    response = api_client.post(
        url, {"email": "no@no.com", "password": "xxx"}, format="json"
    )
    assert response.status_code == 400

@pytest.mark.django_db
def test_update_initial_reps(auth_client, user):
    url = reverse('update_initial_reps')
    res = auth_client.post(url, {"initial_reps": 20})
    assert res.status_code == 200
    user.refresh_from_db()
    assert user.initial_reps == 20

@pytest.mark.django_db
def test_user_profile(auth_client, user):
    url = reverse('user_profile')
    res = auth_client.get(url)
    assert res.status_code == 200
    assert res.data["email"] == user.email
    assert "rank" in res.data

@pytest.mark.django_db
def test_user_rankings(auth_client, user):
    url = reverse('user_rankings')
    res = auth_client.get(url)
    assert res.status_code == 200
    assert isinstance(res.data, list)
