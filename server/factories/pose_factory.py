import factory
from pose.models import PoseAnalysis
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory, SessionFactory
from schedule.models import ActivityType

class PoseAnalysisFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = PoseAnalysis

    user = factory.SubFactory(UserFactory)
    schedule = factory.SubFactory(ScheduleFactory)
    session = factory.SubFactory(SessionFactory)
    activity = ActivityType.SQUAT
    image_url = factory.Faker("image_url")
    pose_data = factory.LazyFunction(lambda: {
        "joint_angles": {"left_knee": 90, "right_knee": 95},
        "keypoints_2d": [[0, 0], [1, 1], [2, 2]]
    })
    ai_comment = factory.LazyFunction(lambda: {
        "good_points": ["허리가 곧습니다"],
        "improvement_points": ["무릎 각도 개선 필요"],
        "improvement_methods": ["천천히 앉기"]
    })
