# schedule/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path('', views.schedule_list, name='schedule_list'),
    path('schedules/', views.user_schedule, name='user_schedule'),
    path('history/', views.user_history, name='user_history'),
]
