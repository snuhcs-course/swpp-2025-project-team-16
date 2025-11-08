# urlpatterns = [
#     path('', views.schedule_list, name='schedule_list'),
#     path('schedule/', views.user_schedule, name='user_schedule'),
#     path('history/', views.user_history, name='user_history'),
#     path('generate_schedule/', views.generate_schedule, name='generate_schedule'),
#     path('schedules/', views.get_user_schedules, name='get_user_schedules'),
#     path('stats/', views.user_stats, name='user_stats')
# ]

from django.urls import path
from . import views

urlpatterns = [
    path('schedules/', views.schedules_view, name='schedules_view'),
    path('schedules/auto-generate/', views.schedules_auto_generate, name='schedules_auto_generate'),
    path('schedules/<int:schedule_id>/', views.schedule_edit, name='schedule_edit'),
    path('sessions/', views.start_session, name='start_session'),
    path('sessions/<int:session_id>/', views.end_session, name='end_session'),
]