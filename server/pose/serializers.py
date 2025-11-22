from rest_framework import serializers
from .models import PoseAnalysis

class PoseAnalysisSerializer(serializers.ModelSerializer):
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
        ]
        read_only_fields = ['id', 'user', 'created_at']
