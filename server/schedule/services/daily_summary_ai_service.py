import json
import logging
from django.utils import timezone
from django.conf import settings
from schedule.models import DailySummary, Session, Schedule, ACTIVITY_TYPE_MAP
from openai import OpenAI, OpenAIError

logger = logging.getLogger(__name__)
client = OpenAI(api_key=settings.OPENAI_API_KEY)


class DailySummaryError(Exception):
    pass


class APIResponseError(DailySummaryError):
    pass


class GenerationError(DailySummaryError):
    pass

def extract_target_done(schedule):
    activity_type = ACTIVITY_TYPE_MAP.get(schedule.activity)
    if activity_type == "reps":
        unit = "reps"
        target_value = schedule.reps_target
        done_value = schedule.reps_done or 0
    elif activity_type == "duration":
        unit = "seconds"
        target_value = int(schedule.duration_target.total_seconds())
        done_value = int(schedule.duration_done.total_seconds()) if schedule.duration_done else 0
    else:
        raise ValueError("Schedule has no target")
    
    return unit, target_value, done_value

def generate_daily_summary(user, date):
    try:
        daily_summaries = DailySummary.objects.filter(user=user, date__lt=date)
        sessions = Session.objects.filter(user=user, created_at__date=date, schedule__isnull=True)
        schedules = Schedule.objects.filter(user=user, scheduled_date=date)

        daily_text = "\n".join([f"- {ds.date} | {ds.summary_text}" for ds in daily_summaries]) or "- None"
        session_text = "\n".join([
            f"- {s.created_at.time()} | {s.activity} | {s.reps_count}" 
            if ACTIVITY_TYPE_MAP.get(s.activity) == "reps"
            else f"- {s.created_at.time()} | {s.activity} | {s.duration.total_seconds()}"
            for s in sessions
        ]) or "- None"
 
        schedule_text_list = []
        for sc in schedules:
            try:
                unit, target_value, done_value = extract_target_done(sc)
                schedule_text_list.append(f"- {sc.scheduled_date} | {sc.activity} | {done_value} / {target_value} {unit}")
            except ValueError as e:
                logger.warning(f"Schedule {sc.id} has no valid target: {e}")
                continue
        schedule_text = "\n".join(schedule_text_list) or "- None"

        prompt = f"""
        You are an assistant generating a daily summary for a user.
        Always start the summary with a short motivational phrase (heroic, uplifting).
        Avoid repeating wording exactly from previous daily summaries.

        Date: {date}

        Previous daily summaries:
        {daily_text}

        Sessions:
        {session_text}

        Schedules:
        {schedule_text}

        Write a friendly, concise daily summary in one line for the user.
        """

        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You summarize the user's daily activity."},
                {"role": "user", "content": prompt},
            ],
            max_tokens=300,
        )

        if not getattr(response, "choices", None):
            raise APIResponseError("Invalid API response: no choices returned")

        try:
            generated_text = response.choices[0].message.content.strip()
        except (AttributeError, IndexError) as e:
            raise APIResponseError(f"Invalid API response: {e}") from e

        return generated_text
        
    except APIResponseError:
        raise
    except OpenAIError as e:
        raise DailySummaryError(f"OpenAI API error: {str(e)}") from e
    except Exception as e:
        raise GenerationError(f"Unexpected error: {str(e)}") from e

def generate_daily_summaries_for_user(user):
    today = timezone.localdate()

    session_dates = {timezone.localtime(s.created_at).date() for s in Session.objects.filter(user=user)}
    schedule_dates = {s.scheduled_date for s in Schedule.objects.filter(user=user)}
    candidate_dates = sorted(session_dates | schedule_dates)
    
    candidate_dates = [d for d in candidate_dates if d < today]
    print(f"User {user.id} - candidate dates for summary: {candidate_dates}")

    summaries = []
    for date in candidate_dates:
        if DailySummary.objects.filter(user=user, date=date).exists():
            print(f"User {user.id} - summary already exists for {date}, skipping")
            continue
        
        print(f"User {user.id} - generating summary for {date}")
        try:
            text = generate_daily_summary(user, date)
            DailySummary.objects.update_or_create(
                user=user,
                date=date,
                defaults={"summary_text": text},
            )
            summaries.append({"date": date, "summary": text})
            print(f"User {user.id} - successfully generated summary for {date}")

        except DailySummaryError as e:
            logger.error(f"Daily summary generation failed for user {user.id} on {date}: {e}")
        except GenerationError as e:
            logger.error(f"Unexpected error for user {user.id} on {date}: {e}")

    return summaries
