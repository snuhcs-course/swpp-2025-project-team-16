import factory
from datetime import date, time, timedelta
from django.utils import timezone
from schedule.models import Schedule, Session, Feedback
from factories.user_factory import UserFactory

class ScheduleFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Schedule

    user = factory.SubFactory(UserFactory)
    scheduled_date = factory.LazyFunction(lambda: timezone.localdate())
    start_time = time(hour=8, minute=0)
    end_time = time(hour=8, minute=30)
    activity = 'squat'
    reps_target = 20
    duration_target = None
    status = 'planned'

class SessionFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Session

    user = factory.SubFactory(UserFactory)
    activity = 'squat'
    reps_count = 10
    duration = timedelta(minutes=10)
    schedule = factory.SubFactory(ScheduleFactory)

class FeedbackFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Feedback

    user = factory.SubFactory(UserFactory)
    schedule = factory.SubFactory(ScheduleFactory)
    summary_text = "Great job!"
