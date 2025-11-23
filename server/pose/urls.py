from django.urls import path
from . import views

urlpatterns = [
    # path('evaluate_posture/', views.evaluate_posture, name='evaluate_posture'),
    path('upload', views.evalute_posture, name='evalute_posture'),
    path('', views.pose_analyses_view, name='pose_analyses_view'),
    path('<int:id>/', views.pose_analysis_detail, name='pose_analysis_detail'),
    path('session/<int:session_id>/', views.pose_analyses_by_session, name='pose_analyses_by_session'),
    path('schedule/<int:schedule_id>/', views.pose_analyses_by_schedule, name='pose_analyses_by_schedule'),
]
