from rest_framework import serializers
from .models import PoseAnalysis

class PoseAnalysisSerializer(serializers.ModelSerializer):
    good_points = serializers.SerializerMethodField()

    class Meta:
        model = PoseAnalysis
        fields = ['id', 'user', 'session', 'schedule', 'image_url', 'pose_data', 'ai_comment', 'created_at', 'good_points']
        read_only_fields = ['id', 'user', 'created_at']

    def get_good_points(self, obj):
        pose_data = getattr(obj, 'pose_data', {})
        return pose_data.get('good_points', 0)

    class Meta:
        model = PoseAnalysis
        fields = [
            'id',
            'user',
            'session',
            'image_url',
            'pose_data',
            'ai_comment',
            'created_at',
            'good_points'
        ]
        read_only_fields = ['id', 'user', 'created_at']
    
    def get_good_points(self, obj):
        pose_data = obj.pose_data or {}
        return pose_data.get('good_points', "")
