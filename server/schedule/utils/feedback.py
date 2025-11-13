from datetime import timedelta
from django.conf import settings
from openai import OpenAI

client = OpenAI(api_key=settings.OPENAI_API_KEY)


def generate_feedback_from_schedule(user, schedule):
    if schedule.reps_target:
        target_value = schedule.reps_target
        done_value = schedule.reps_done or 0
        prompt_detail = f"Target: {target_value} reps, Done: {done_value} reps"
    elif schedule.duration_target:
        target_value = int(schedule.duration_target.total_seconds())
        done_value = int(schedule.duration_done.total_seconds()) if schedule.duration_done else 0
        prompt_detail = f"Target: {target_value} secs, Done: {done_value} secs"
    else:
        prompt_detail = "No reps or duration data available"
    
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
        return f"Feedback generation failed: {str(e)}"
