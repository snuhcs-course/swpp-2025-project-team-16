from django.urls import path
from . import views

urlpatterns = [
    path("check_email/", views.check_email),
    path("signup/", views.signup),
    path("login/", views.login),
    path("update_progress/", views.update_progress),
]
