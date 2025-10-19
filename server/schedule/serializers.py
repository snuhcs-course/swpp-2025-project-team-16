from rest_framework import serializers
from .models import Schedule

class ScheduleSerializer(serializers.ModelSerializer):
    class Meta:
        model = Schedule
        fields = [
            'id',
            'user',       
            'session',     
            'date',
            'start_time',   
            'end_time',
            'name',
            'is_finished'   
        ]
