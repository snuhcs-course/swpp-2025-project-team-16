from django.db import models
from django.conf import settings  # accounts.Account 참조용

class Sport(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField()
    total_sessions = models.IntegerField()

    def __str__(self):
        return self.name


class Session(models.Model):
    user = models.ManyToManyField(settings.AUTH_USER_MODEL, related_name='sessions')
    title = models.CharField(max_length=200)
    description = models.TextField()
    sport = models.ForeignKey(Sport, on_delete=models.CASCADE, related_name='sessions')
    difficulty_level = models.CharField(max_length=50)
    length = models.IntegerField(help_text="Length in minutes")
    previous_session = models.ForeignKey('self', on_delete=models.SET_NULL, null=True, blank=True, related_name='next_sessions')

    def __str__(self):
        return self.title


class Schedule(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='schedules')
    session = models.ForeignKey(Session, on_delete=models.CASCADE)
    date = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    name = models.CharField(max_length=200)
    is_finished = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.user.email} - {self.name}"


class SportStatus(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='sport_statuses')
    sport = models.ForeignKey(Sport, on_delete=models.CASCADE)
    session = models.ForeignKey(Session, on_delete=models.CASCADE, related_name='sport_statuses')
    proficiency_level = models.CharField(max_length=50)
    last_practiced = models.DateField()

    def __str__(self):
        return f"{self.user.email} - {self.sport.name}"
