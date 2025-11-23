import pytest
from rest_framework.test import APIClient
from factories.user_factory import UserFactory

@pytest.fixture
def user(db):
    return UserFactory()

@pytest.fixture
def api_client():
    return APIClient()

@pytest.fixture
def auth_client(user):
    client = APIClient()
    client.force_authenticate(user=user)
    return client
