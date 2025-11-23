import pytest
from accounts.models import Account

# @pytest.mark.django_db
# def test_create_user():
#     user = Account.objects.create_user(
#         email="a@a.com",
#         name="Alice",
#         password="1234"
#     )
#     assert user.email == "a@a.com"
#     assert user.check_password("1234")
#     assert user.level == 1
#     assert user.xp == 0


@pytest.mark.django_db
def test_create_user():
    user = Account.objects.create_user(
        email="test@example.com",
        name="Tester",
        password="password123"
    )
    assert user.email == "test@example.com"
    assert user.name == "Tester"
    assert user.check_password("password123")
    assert user.is_active

@pytest.mark.django_db
def test_create_superuser():
    admin = Account.objects.create_superuser(
        email="admin@example.com",
        name="Admin",
        password="admin123"
    )
    assert admin.is_staff
    assert admin.is_superuser