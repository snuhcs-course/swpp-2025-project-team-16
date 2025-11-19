from datetime import timedelta
from django.db import models
from django.conf import settings
from django.core.exceptions import ValidationError

class ActivityType(models.TextChoices):
    SQUAT = 'squat', 'Squat'
    PLANK = 'plank', 'Plank'
    LUNGE = 'lunge', 'Lunge'

ACTIVITY_TYPE_MAP = {
    ActivityType.SQUAT: 'reps',
    ActivityType.PLANK: 'duration',
    ActivityType.LUNGE: 'reps'
}

def validate_activity_fields(activity, reps=None, duration=None):
    expected_field = ACTIVITY_TYPE_MAP.get(activity)
    
    reps_val = reps if reps else None
    duration_val = None
    if duration is not None:
        if isinstance(duration, timedelta):
            duration_val = duration if duration.total_seconds() > 0 else None
        else:
            duration_val = duration
    
    if (reps_val is None and duration_val is None) or (reps_val is not None and duration_val is not None):
        raise ValidationError("Must have either reps or duration, not both.")
    
    if expected_field == 'reps':
        if reps_val is None:
            raise ValidationError(f"{activity} requires reps.")
        if duration_val is not None:
            raise ValidationError(f"{activity} cannot have duration.")
    elif expected_field == 'duration':
        if duration_val is None:
            raise ValidationError(f"{activity} requires duration.")
        if reps_val is not None:
            raise ValidationError(f"{activity} cannot have reps.")
    else:
        raise ValidationError(f"Unknown activity type: {activity}")

def zero_duration():
    return timedelta(seconds=0)

class Schedule(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='schedules')
    scheduled_date = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    activity = models.CharField(max_length=20, choices=ActivityType.choices)
    reps_target = models.IntegerField(null=True, blank=True)
    reps_done = models.IntegerField(null=True, blank=True, default=0)
    duration_target = models.DurationField(null=True, blank=True)
    duration_done = models.DurationField(null=True, blank=True, default=zero_duration)
    status = models.CharField(max_length=20, choices=[('planned','Planned'),('completed','Completed'),('partial','Partial'),('missed','Missed')], default='planned')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [models.Index(fields=['user', 'scheduled_date'])]

class Session(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='sessions')
    activity = models.CharField(max_length=20, choices=ActivityType.choices)
    reps_count = models.IntegerField(null=True, blank=True)
    duration = models.DurationField(null=True, blank=True)
    schedule = models.ForeignKey(Schedule, on_delete=models.CASCADE, related_name='sessions', null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=['user'])]

class Feedback(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='feedbacks')
    schedule = models.OneToOneField(Schedule, on_delete=models.CASCADE, related_name='feedback')
    summary_text = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=['user'])]
