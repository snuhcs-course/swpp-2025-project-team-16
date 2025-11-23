import factory
from pose.models import PoseAnalysis
from factories.user_factory import UserFactory
from factories.schedule_factory import ScheduleFactory, SessionFactory

class PoseAnalysisFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = PoseAnalysis

    user = factory.SubFactory(UserFactory)
    schedule = factory.SubFactory(ScheduleFactory)
    session = factory.SubFactory(SessionFactory)
    image_url = factory.Faker("image_url")
    pose_data = factory.LazyFunction(lambda: {"keypoints": [[0, 0, 0]]})
    ai_comment = factory.Faker("sentence")
