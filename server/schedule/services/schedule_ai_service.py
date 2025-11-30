import json
from django.utils import timezone
from django.conf import settings
from datetime import datetime, date
from schedule.models import Schedule, ACTIVITY_TYPE_MAP
from openai import OpenAI, OpenAIError

client = OpenAI(api_key=settings.OPENAI_API_KEY)

class AIScheduleError(Exception):
    pass

def generate_daily_schedules(user, date):
    recent_schedules = Schedule.objects.filter(user=user).order_by('-scheduled_date')[:5]
    history = []
    for s in recent_schedules:
        history_item = {
            "scheduled_date": str(s.scheduled_date),
            "start_time": s.start_time.strftime("%H:%M"),
            "end_time": s.end_time.strftime("%H:%M"),
            "activity": s.activity,
            "status": s.status
        }

        activity_type = ACTIVITY_TYPE_MAP.get(s.activity)

        if activity_type == "reps":
            history_item["reps_target"] = s.reps_target
            history_item["reps_done"] = s.reps_done or 0
        elif activity_type == "duration":
            history_item["duration_target"] = int(s.duration_target.total_seconds())
            history_item["duration_done"] = int(s.duration_done.total_seconds()) if s.duration_done else 0

        history.append(history_item)
    
    initial_reps = getattr(user, 'initial_reps', 0)

    activity_rule = generate_activity_rule(ACTIVITY_TYPE_MAP)

    prompt = generate_scheduling_prompt(
        date=date,
        initial_reps=initial_reps,
        activity_rule=activity_rule,
        history_data=history
    )

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are a professional fitness coach. Always respond with valid JSON only."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=1000,
            )
        ai_output = response.choices[0].message.content.strip()
        parsed = json.loads(ai_output)
        return parsed
    
    except json.JSONDecodeError as e:
        raise AIScheduleError("AI returned invalid JSON") from e
    except OpenAIError as e:
        raise AIScheduleError("OpenAI API error") from e
    except Exception as e:
        raise AIScheduleError("Unexpected error during schedule generation") from e


def generate_activity_rule(activity_map: dict) -> str:
    reps_based = [a.value for a, t in activity_map.items() if t == 'reps']
    duration_based = [a.value for a, t in activity_map.items() if t == 'duration']
    activity_rule = (
        f"Available activities:\n"
        f"- Reps-based: {', '.join(reps_based)} (use 'reps_target' field)\n"
        f"- Duration-based: {', '.join(duration_based)} (use 'duration_target' field in seconds)"
    )
    return activity_rule


def generate_scheduling_prompt(
    date: datetime.date,
    initial_reps: int,
    activity_rule: str,
    history_data: dict = None
    ):

    now_local = timezone.localtime()
    now_str = now_local.isoformat()
    
    return f"""
        You are a professional fitness coach tasked with generating safe, realistic, and personalized workout schedules.

        User Info:
        - Baseline squat ability: {initial_reps} reps
        - Recent history: {json.dumps(history_data, indent=2) if history_data else "No history available"}

        Task:
        Generate realistic daily workout schedule(s) for the date {date}.

        Activity Rules:
        {activity_rule}

        Scheduling Rules:
        1. Each schedule MUST include EITHER 'reps_target' OR 'duration_target' — never both.
        2. Apply progressive overload gently:
            - If the user's recent status shows 'missed' or 'partial', reduce the difficulty.
            - Even for 'completed' history, increase difficulty only slightly; avoid big jumps.
            - Do NOT assign excessively high volumes (e.g., "200 squats").  
              If high total volume is needed, it must be split across multiple schedules —  
              never in a single schedule.
            - Plank duration must not exceed 90 seconds; around 60 seconds is considered ideal.
        3. Vary the selected activities to prevent overtraining and to keep workouts interesting.
        4. **Flexible Workout Window**:  
           The workout duration is often short (10-20 minutes), but set a wide time window  
           (e.g., 60-90 minutes) between 'start_time' and 'end_time' to give the user flexibility.
        5. **Time Distribution Across the Day**:  
           Spread schedules across reasonable times (morning, midday, evening).  
           Avoid unrealistic times, such as 03:00 AM.
        6. **Conservative Quantity**:  
           Because users cannot delete or edit schedules, generate only reasonable number of schedules.  
           Keep them manageable to prevent unnecessary "missed" statuses.
        7. **No Past Schedules**:  
           All generated schedules must be **after the current time** ({now_str}).  
           Do NOT generate any schedule set in the past, including earlier times on the same day.  
           If the current time has already passed typical workout windows for today,  
           schedule them for the next valid day instead.
        8. Follow the output *structure* below, but do NOT reuse example values.  
           The output must be valid JSON (no markdown, no comments).

        Output Format (strict JSON):
        {{
            "schedules": [
                {{
                    "scheduled_date": "YYYY-MM-DD",
                    "start_time": "08:00",
                    "end_time": "09:00",
                    "activity": "squat",
                    "reps_target": 25
                }},
                {{
                    "scheduled_date": "YYYY-MM-DD",
                    "start_time": "18:30",
                    "end_time": "19:30",
                    "activity": "plank",
                    "duration_target": 60
                }}
            ]
        }}
        """