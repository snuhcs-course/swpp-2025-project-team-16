from django.urls import path
from . import views

urlpatterns = [
    path("check_email/", views.check_email),
    path("signup/", views.signup),
    path("login/", views.login),
    path('rankings/',  views.user_rankings, name='user_rankings'),
    path('update_initial_reps/', views.update_initial_reps, name='update_initial_reps'),
    path('', views.user_profile, name='user_profile')
]
