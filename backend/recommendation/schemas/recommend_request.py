from pydantic import BaseModel, validator
from typing import Optional

class RecommendRequest(BaseModel):
    user_id: str
    equipment: str
    user_level: int
    requirements: Optional[str] = None
    top_k: Optional[int] = -1

    @validator("user_level")
    def check_level(cls, v):
        if not (1 <= v <= 3):
            raise ValueError("user_level must be between 1 and 3")
        return v