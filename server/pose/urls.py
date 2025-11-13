# pose/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path('evaluate_posture/', views.evaluate_posture, name='evaluate_posture'),
]
