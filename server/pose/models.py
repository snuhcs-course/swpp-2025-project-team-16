from django.db import models
from django.conf import settings
from schedule.models import ActivityType

class PoseAnalysis(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    schedule = models.ForeignKey('schedule.Schedule', on_delete=models.CASCADE, null=True, blank=True)
    session = models.ForeignKey('schedule.Session', on_delete=models.CASCADE, null=True, blank=True)
    activity = models.CharField(max_length=20, choices=ActivityType.choices)
    image_url = models.URLField(blank=True, null=True)
    pose_data = models.JSONField(blank=True, null=True)
    ai_comment = models.JSONField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=['user'])]
