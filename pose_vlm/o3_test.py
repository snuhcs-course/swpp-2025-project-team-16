from openai import OpenAI
from models.utils import image_to_base64

from langchain_community.callbacks import get_openai_callback

import os
OPENAI_API_KEY = os.getenv("./models/.env")

client = OpenAI(OPENAI_API_KEY)# 또는 환경 변수 자동 인식
image_base64 = image_to_base64("sample_data/squat_correct_1_skeleton.png")


image_url = f"data:image/jpeg;base64,{image_base64}"

squat_instruction = """
                    Bodyweight Squat Evaluation Rubric (Image-based, Total: 100 points)

                    Score the visible frame using these 4 categories (25 pts each):

                    1. Stance & Foot Pressure (25 pts)
                    - Feet hip~shoulder width, toes slightly outward.
                    - Both heels stay down; full foot is planted (toes / arch / heel).
                    - Weight looks evenly distributed, not only on toes or only on heels.

                    2. Knee and Hip Tracking (25 pts)
                    - Knees point in the same direction as the toes or slightly outward (NOT caving inward).
                    - Hips are opened/externally rotated instead of collapsing inward.
                    - Hips and knees are bent together with control, not just dumping into the lower back.

                    3. Pelvis, Core, and Spine Control (25 pts)
                    - Core appears braced (torso not loose or collapsing).
                    - Back is neutral: no obvious over-arch and no rounding.
                    - Pelvis is stable: no visible early posterior tilt / “butt wink.”
                    - Chest is open. The torso may lean forward slightly, but it must NOT collapse heavily forward or dump load into the lower back.

                    4. Depth & Bottom Position (25 pts)
                    - Squat depth is appropriate for the person’s mobility (thighs at least near parallel or below if safe).
                    - Spine still looks neutral at this depth.
                    - Position at the bottom looks controlled, not jammed/bounced.

                    After scoring the 4 categories (0–100), apply safety penalties (subtract from the total):
                    -20 if the pelvis tucks under / lower back rounds at the bottom ("butt wink").
                    -15 if the torso collapses forward and shifts load into the lower back.
                    -15 if the knees collapse inward (not aligned with toes).
                    -10 if the heels lift or balance is clearly dumped into the toes.

                    These penalties are intentionally large: unsafe form must receive a much lower final score than a form that is simply less deep or less "pretty".
                    """



response = client.chat.completions.create(
    model="o4-mini",
    reasoning_effort="medium",
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "PostureEvaluationResult",
            "schema": {
                "type": "object",
                "properties": {
                    "score": {"type": "integer"},
                    "feedback": {"type": "string"}
                },
                "required": ["score", "feedback"]
            }
        }
    },
    messages=[
        {"role": "system", "content": """
                            You are a professional fitness posture evaluator.
                            Your role is to critically evaluate a user's exercise posture to help them 
                            improve their long-term training habits and movement patterns.

                            Be **strict, objective, prioritizing **injury prevention and biomechanical precision**
                            - Do not sugarcoat mistakes — point them out clearly.
                            - Focus on posture safety, balance, and efficiency.
                            - Always provide concrete, actionable advice that the user can apply immediately.
            """},
        {"role": "user", "content": [
            {"type": "text", "text": """
                                    Analyze the user's posture based on:
                                    1. The provided image.
                                    2. The text description below.

                                    Consider joint angles, symmetry, alignment, balance, and muscular engagement.

                                    You may find skeleton in the image useful.

                                    Respond **only in structured format**:
                                    - score: an integer between 0 and 100 (10 = perfect form)
                                    - feedback: 2–4 lines of feedback that directly explain good points and what to fix.

                                    Evaluation Criteria:
                                    {squat_instruction}
                                    """},
            {"type": "image_url", "image_url": {"url": image_url}}
        ]}
    ]
)

usage_info = {
    "prompt_tokens": response.usage.prompt_tokens,
    "completion_tokens": response.usage.completion_tokens,
    "total_tokens": response.usage.total_tokens,
}

print(response.choices[0].message)
print(usage_info)

input_price_per_1M = 1.10    # 미국 달러, 입력 토큰 1 백만당
output_price_per_1M = 4.40   # 미국 달러, 출력 토큰 1 백만당

prompt_cost     = response.usage.prompt_tokens     / 1_000_000 * input_price_per_1M
completion_cost = response.usage.completion_tokens / 1_000_000 * output_price_per_1M
total_cost      = prompt_cost + completion_cost


print(f"Total estimated cost: ${total_cost:.5f}")