from datetime import datetime, timedelta
from django.utils.timezone import make_aware
from django.conf import settings
from openai import OpenAI
from schedule.models import Schedule, Session, DailySummary
import logging

logger = logging.getLogger(__name__)
client = OpenAI(api_key=settings.OPENAI_API_KEY)

def extract_target_done(schedule):
    if schedule.reps_target:
        unit = "reps"
        target_value = schedule.reps_target
        done_value = schedule.reps_done or 0
    elif schedule.duration_target:
        unit = "seconds"
        target_value = int(schedule.duration_target.total_seconds())
        done_value = int(schedule.duration_done.total_seconds()) if schedule.duration_done else 0
    else:
        raise ValueError("Schedule has no target")
    
    return unit, target_value, done_value

def generate_feedback_from_schedule(user, schedule):
    unit, target_value, done_value = extract_target_done(schedule)
    
    prompt_detail = f"""
        You are an AI fitness coach.

        ### User & Workout Information
        - User name: {user.name}
        - Activity: {schedule.activity}
        - Date: {schedule.scheduled_date}
        - Time: {schedule.start_time.strftime('%H:%M')} - {schedule.end_time.strftime('%H:%M')}

        ### Performance
        - Target: {target_value} {unit}
        - Completed: {done_value} {unit}

        ### Additional Context
        - This session is linked to a scheduled workout.
        - Provide:
            1) A **short, encouraging one-sentence summary** (suitable for a mobile toast message).
                - It MUST be the first line.
                - It must be concise and motivating.
            - Examples:
                "🎉 Excellent work! You hit your target of 20 squats!"
                "Great effort! You completed 15 out of 20 squats (75%)."
            2) A **longer, more detailed feedback section**.
                - Include performance evaluation.
                - Include motivation.
                - If target was not achieved, suggest a realistic next goal.
                - If long-term progress data is provided, reference it.

        ### Output Format
        - First line: short feedback
        - After a blank line: detailed feedback (1-3 paragraphs)
        - Do NOT wrap in JSON. Do NOT include markdown.
    """
    
    prompt = (
        f"User {user.name} performed activity '{schedule.activity}' "
        f"on {schedule.scheduled_date} from {schedule.start_time.strftime('%H:%M')} "
        f"to {schedule.end_time.strftime('%H:%M')}. "
        f"{prompt_detail}. "
        "Write a concise, encouraging feedback summary."
    )

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            max_tokens=100,
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        logger.error(f"Feedback generation failed: {str(e)}")
        return f"Feedback generation failed: {str(e)}"

def generate_daily_summary(user, date):
    try:
        daily_summaries = DailySummary.objects.filter(
            user=user,
            date=date
        )

        sessions = Session.objects.filter(
            user=user,
            created_at__date=date,
            schedule__isnull=True
        )

        schedules = Schedule.objects.filter(
            user=user,
            scheduled_date=date
        )

        daily_summaries_texts = "\n".join([
            f"- {ds.date} | {ds.summary_text}" 
            for ds in daily_summaries
        ]) if daily_summaries.exists() else "- None"
        
        session_texts = "\n".join([
            f"- {s.created_at.time()} | {s.activity} | {s.reps_count or s.duration}" 
            for s in sessions
        ]) if sessions.exists() else "- None"
        
        schedule_texts_list = []
        for sc in schedules:
            try:
                unit, target_value, done_value = extract_target_done(sc)
                schedule_texts_list.append(
                    f"- {sc.scheduled_date} | {sc.activity} | {done_value} / {target_value} {unit}"
                )
            except ValueError as e:
                logger.warning(f"Schedule {sc.id} has no valid target: {e}")
                continue

        schedule_texts = "\n".join(schedule_texts_list) if schedule_texts_list else "- None"
        
        # 프롬프트 생성
        prompt = f"""
            You are an assistant generating a daily summary for a user.
            Always start the summary with a short motivational phrase (heroic, uplifting).
            Avoid repeating wording exactly from previous daily summaries.
            Example motivational phrases:
            - Phenomenal performance! You are unstoppable!
            - Great effort! You are getting stronger.
            - Excellent execution! Your form was impeccable.
            
            Date: {date}

            Previous daily summaries (content only, do not repeat the wording exactly):
            {daily_summaries_texts}

            Sessions:
            {session_texts}

            Schedules:
            {schedule_texts}

            Write a friendly, concise daily summary in one line for the user based on their sessions and schedules.
        """

        logger.info(f"Generating daily summary for user {user.id} on {date}")
        
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You summarize the user's daily activity."},
                {"role": "user", "content": prompt},
            ],
            max_tokens=300,
        )
        
        generated_text = response.choices[0].message.content.strip()
        
        logger.info(f"Successfully generated daily summary for user {user.id}")
        return generated_text
        
    except AttributeError as e:
        error_msg = f"API response parsing error: {str(e)}"
        logger.error(error_msg)
        return f"Failed to generate summary: {error_msg}"
    
    except Exception as e:
        error_msg = f"Unexpected error: {str(e)}"
        logger.error(f"Daily summary generation failed for user {user.id}: {error_msg}")
        return f"Failed to generate summary: {error_msg}"
