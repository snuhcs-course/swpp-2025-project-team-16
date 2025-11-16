from rest_framework import serializers
from .models import Schedule, Session, Feedback, validate_activity_fields
from datetime import timedelta

class ScheduleSerializer(serializers.ModelSerializer):
    duration_target = serializers.SerializerMethodField()
    duration_done = serializers.SerializerMethodField()

    class Meta:
        model = Schedule
        fields = [
            'id', 'user', 'scheduled_date', 'start_time', 'end_time', 'activity',
            'reps_target', 'reps_done', 'duration_target', 'duration_done', 'status',
            'created_at', 'updated_at'
        ]
        read_only_fields = ('user', 'reps_done', 'duration_done', 'status', 'created_at', 'updated_at')

    def get_duration_target(self, obj):
        return int(obj.duration_target.total_seconds()) if obj.duration_target else None

    def get_duration_done(self, obj):
        return int(obj.duration_done.total_seconds()) if obj.duration_done else None

    def to_internal_value(self, data):
        # duration_target 입력은 초 단위 int → timedelta
        duration_seconds = data.get('duration_target')
        if duration_seconds is not None:
            try:
                data['duration_target'] = timedelta(seconds=int(duration_seconds))
            except ValueError:
                raise serializers.ValidationError({"duration_target": "Must be an integer (seconds)."})
        return super().to_internal_value(data)

    def validate(self, data):
        activity = data.get('activity', getattr(self.instance, 'activity', None))
        reps = data.get('reps_target', getattr(self.instance, 'reps_target', None))
        duration = data.get('duration_target', getattr(self.instance, 'duration_target', None))
        validate_activity_fields(activity, reps=reps, duration=duration)
        return data

class SessionSerializer(serializers.ModelSerializer):
    duration = serializers.SerializerMethodField()

    class Meta:
        model = Session
        fields = ['id', 'user', 'activity', 'reps_count', 'duration', 'schedule', 'created_at']
        read_only_fields = ('user', 'schedule', 'reps_count', 'duration', 'created_at')

    def get_duration(self, obj):
        return int(obj.duration.total_seconds()) if obj.duration else None
    
    def clean(self):
        validate_activity_fields(activity=self.activity,
                                 reps=self.reps_count,
                                 duration=self.duration)

class FeedbackSerializer(serializers.ModelSerializer):
    class Meta:
        model = Feedback
        fields = ['id', 'user', 'schedule', 'summary_text', 'created_at']
        read_only_fields = ('user', 'schedule', 'summary_text', 'created_at')
