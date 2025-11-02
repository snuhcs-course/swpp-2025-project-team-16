import os
import requests
from typing import Dict, Any
from django.conf import settings
from pydantic import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_community.callbacks import get_openai_callback

import logging
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)


# ✅ 1️⃣ 출력 구조 정의 (10점 만점 점수 + 피드백)
class PostureEvaluationResult(BaseModel):
    """
    Output schema for posture evaluation result.
    """
    score: int = Field(
        description="Posture score on a 0 – 100 scale"
    )
    feedback: str = Field(
        description="2–4 lines of natural language feedback about the posture, focusing on actionable advice."
    )

class SummaryResult(BaseModel):
    """
    Output schema for a 3-part English summary.
    """
    good_points: str = Field(
        description="1 - 2 sentences describing strengths."
    )
    improvement_points: str= Field(
        description="1 - 2 sentences describing areas for improvement."
    )
    improvement_methods: str = Field(
        description="1 - 2 sentences actionable suggestions to improve."
    )


class ScorenResult(BaseModel):
    """
    Output schema for posture evaluation result.
    """
    score: int = Field(
        description="Posture score on a 0 – 100 scale"
    )


# ✅ 2️⃣ VLM 서비스 클래스
class VLMService:
    """
    Service for Vision-Language posture evaluation.
    """

    def __init__(self):
        pass

    def evaluate_posture(self, image_base64: str, description_text: str, human_opinion_text:str) -> Dict[str, Any]:
        """
        Evaluate posture quality based on an image and a textual description.

        Args:
            image_base64: Base64-encoded image string (JPEG or PNG)
            description_text: Text description (e.g. joint angles, posture type)

        Returns:
            Dict[str, Any]: Contains score (0–10) and feedback (text)
        """
        try:
            if not image_base64:
                raise ValueError("image_base64 is required")

            # Construct image URL for the model
            image_url = f"data:image/jpeg;base64,{image_base64}"

            # ✅ Prompt Template
            prompt = ChatPromptTemplate.from_messages([
                        (
                            "system",
                            """
                            You are a **professional fitness posture evaluator**.

                            Your role is to critically evaluate a user's exercise posture to help them 
                            improve their long-term training habits and movement patterns.

                            Be **strict, objective, prioritizing **injury prevention and biomechanical precision**
                            - Do not sugarcoat mistakes — point them out clearly.
                            - Focus on posture safety, balance, and efficiency.
                            - Always provide concrete, actionable advice that the user can apply immediately.
                            """
                        ),
                        (
                            "user",
                            [
                                {
                                    "type": "text",
                                    "text": """
                                    Analyze the user's posture based on:
                                    1. The provided image.
                                    2. The text description below.

                                    Consider symmetry, alignment, balance, and muscular engagement.

                                    You may find skeleton in the image useful.

                                    Also, consider the following human expert opinion (but, just as a reference):

                                    Respond **only in structured format**:
                                    - score: an integer between 0 and 100 (10 = perfect form)
                                    - feedback: 2–4 lines of feedback that directly explain good points and what to fix.

                                    Evaluation Criteria:
                                    {description_text}

                                    Huam Opinoon:
                                    {human_opinion_text}
                                    
                                    """
                                },
                                {"type": "image_url", "image_url": {"url": "{image_url}"}}
                            ]
                        )
                    ])


            # ✅ VLM Model with structured output
            llm = ChatOpenAI(model="gpt-5",
                             temperature=1).with_structured_output(PostureEvaluationResult)

            chain = prompt | llm

            logger.info("Running posture evaluation via VLM...")

            # Execute chain
            with get_openai_callback() as cb:
                msg = chain.invoke({
                    "description_text": description_text,
                    "human_opinion_text": human_opinion_text,
                    "image_url": image_url
                })

            # cb 안에 토큰 사용량/비용 정보가 들어 있음
            usage_info = {
                "total_tokens": cb.total_tokens,
                "prompt_tokens": cb.prompt_tokens,
                "completion_tokens": cb.completion_tokens,
                "total_cost_usd": cb.total_cost,
            }
            # msg = chain.invoke({
            #     "description_text": description_text,
            #     "image_url": image_url
            # })

            # ✅ Return structured response
            return {
                "score": msg.score,
                "feedback": msg.feedback,
                "usage_info": usage_info
            }
        
        

        except Exception as e:
            logger.error(f"VLM evaluation failed: {str(e)}")
            raise Exception(f"Posture evaluation failed: {str(e)}")


    def summarize(self, text: str) -> Dict[str, Any]:
        """
        Summarize an input text into three English categories:
        - good_points
        - improvement_points
        - improvement_methods
        """
        try:
            if not text or not text.strip():
                raise ValueError("text is required")

            prompt = ChatPromptTemplate.from_messages([
                ("system",
                "You are a concise and precise English summarizer. "
                "Prioritize safety, clarity, and practical advice. "
                "Write short, single-sentence bullets. "
                "Output ONLY the fields defined by the schema."),
                ("user",
                "Summarize the following content into three lists:\n"
                "1) Good points (strengths)\n"
                "2) Areas for improvement\n"
                "3) Actionable suggestions (how to improve)\n\n"
                "Input text:\n{text}")
            ])

            llm = ChatOpenAI(
                model="gpt-4o",
                temperature=0.7,
            ).with_structured_output(SummaryResult)

            chain = prompt | llm

            logger.info("Running summarize via VLM...")

            with get_openai_callback() as cb:
                msg = chain.invoke({"text": text})

            usage_info = {
                "total_tokens": cb.total_tokens,
                "prompt_tokens": cb.prompt_tokens,
                "completion_tokens": cb.completion_tokens,
                "total_cost_usd": cb.total_cost,
            }

            return {
                "good_points": msg.good_points,
                "improvement_points": msg.improvement_points,
                "improvement_methods": msg.improvement_methods,
                "usage_info": usage_info,
            }

        except Exception as e:
            logger.error(f"VLM summarize failed: {str(e)}")
            raise Exception(f"Summarize failed: {str(e)}")

    def evaluate_score(self, description_text: str, feedback_text) -> Dict[str, Any]:
        """
        Evaluate posture quality based on an image and a textual description.

        Args:
            image_base64: Base64-encoded image string (JPEG or PNG)
            description_text: Text description (e.g. joint angles, posture type)

        Returns:
            Dict[str, Any]: Contains score (0–10) and feedback (text)
        """
        try:

            # ✅ Prompt Template
            prompt = ChatPromptTemplate.from_messages([
                        (
                            "system",
                            """
                            You are a **professional fitness posture evaluator**.

                            Your role is to critically evaluate a user's exercise posture to help them 
                            improve their long-term training habits and movement patterns.

                            Be **strict, objective, prioritizing **injury prevention and biomechanical precision**
                            - Do not sugarcoat mistakes — point them out clearly.
                            - Focus on posture safety, balance, and efficiency.
                            - Always provide concrete, actionable advice that the user can apply immediately.
                            """
                        ),
                        (
                            "user",
                            [
                                {
                                    "type": "text",
                                    "text": """
                                    Analyze the user's posture score based on:
                                        1. The feedback text below which one got from his personal trainer using score metric.

                                    Respond **only in structured format**:
                                    - score: an integer between 0 and 100 (10 = perfect form)


                                    Score Metric:
                                    {description_text}

                                    Feedback:
                                    {feedback_text}

                                    """
                                }
                            ]
                        )
                    ])


            # ✅ VLM Model with structured output
            llm = ChatOpenAI(model="gpt-4o").with_structured_output(PostureEvaluationResult)

            chain = prompt | llm

            logger.info("Running posture evaluation via VLM...")

            # Execute chain
            with get_openai_callback() as cb:
                msg = chain.invoke({
                    "description_text": description_text,
                    "feedback_text": feedback_text
                })

            # cb 안에 토큰 사용량/비용 정보가 들어 있음
            usage_info = {
                "total_tokens": cb.total_tokens,
                "prompt_tokens": cb.prompt_tokens,
                "completion_tokens": cb.completion_tokens,
                "total_cost_usd": cb.total_cost,
            }
            # msg = chain.invoke({
            #     "description_text": description_text,
            #     "image_url": image_url
            # })

            # ✅ Return structured response
            return {
                "score": msg.score,
            }

        except Exception as e:
            logger.error(f"VLM evaluation failed: {str(e)}")
            raise Exception(f"Posture evaluation failed: {str(e)}")
