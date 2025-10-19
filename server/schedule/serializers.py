from rest_framework import serializers
from .models import Schedule

class ScheduleSerializer(serializers.ModelSerializer):
    class Meta:
        model = Schedule
        fields = ['id', 'userId', 'sessionId', 'date', 'startTime', 'endTime', 'name', 'isFinished']
        extra_kwargs = {
            'userId': {'read_only': True},  
        }