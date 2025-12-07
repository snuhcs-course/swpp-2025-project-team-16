import factory
from accounts.models import Account

class UserFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Account
        django_get_or_create = ('email',)
        skip_postgeneration_save = True

    email = factory.Sequence(lambda n: f"user{n}@example.com")
    name = factory.Faker("name")
    password = factory.PostGenerationMethodCall('set_password', '1234')

    # postgeneration hook 예시
    @factory.post_generation
    def some_related(self, create, extracted, **kwargs):
        # 필요한 후처리 로직 (예: profile 생성)
        if not create:
            return
        # 수동으로 save 필요하면 여기서 호출
        self.save()
