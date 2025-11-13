from rest_framework import serializers
from .models import Schedule,Exercise, WorkoutPlan


class ExerciseSerializer(serializers.ModelSerializer):
    repTarget = serializers.IntegerField(source='rep_target', allow_null=True)
    repDone = serializers.IntegerField(source='rep_done', allow_null=True)
    order = serializers.IntegerField(required=False)
    xp = serializers.IntegerField(required=False)
    accuracy = serializers.FloatField(required=False, allow_null=True)

    class Meta:
        model = Exercise
        fields = [
            'id',
            'name',
            'description',
            'repTarget',
            'repDone',
            'duration',
            'order',
            'xp',
            'accuracy',
            'status',
        ]

class WorkoutPlanSerializer(serializers.ModelSerializer):
    exercises = ExerciseSerializer(many=True)

    class Meta:
        model = WorkoutPlan
        fields = [
            'id', 'date', 'start_time', 'finish_time',
            'exercises', 'points', 'is_completed', 'feedback'
        ]

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
