from django.urls import path
from . import views

urlpatterns = [
    path('schedules/', views.schedules_view, name='schedules_view'),
    path('schedules/auto-generate/', views.schedules_auto_generate, name='schedules_auto_generate'),
    path('schedules/mark-missed/', views.mark_missed_schedules_view, name='mark_missed_schedules_view'),
    path('sessions/', views.sessions_view, name='sessions_view'),
    path('sessions/start/', views.start_session, name='start_session'),
    path('sessions/<int:session_id>/end/', views.end_session, name='end_session'),
    path('daily-summaries/', views.daily_summaries_view, name='daily_summaries_view'),
    path('daily-summaries/auto-generate/', views.daily_summaries_auto_generate, name='daily_summaries_auto_generate'),
]
