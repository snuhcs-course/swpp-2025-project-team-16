from datetime import timedelta
from django.conf import settings
from openai import OpenAI

client = OpenAI(api_key=settings.OPENAI_API_KEY)


def generate_feedback_from_schedule(user, schedule):
    if schedule.reps_target:
        unit = "reps"
        target_value = schedule.reps_target
        done_value = schedule.reps_done or 0
    elif schedule.duration_target:
        unit = "seconds"
        target_value = int(schedule.duration_target.total_seconds())
        done_value = int(schedule.duration_done.total_seconds()) if schedule.duration_done else 0
    else:
        raise ValueError("Schedule has no target; cannot generate feedback.")
    
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
        return f"Feedback generation failed: {str(e)}"
