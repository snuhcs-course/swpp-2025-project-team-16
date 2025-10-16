from pydantic import BaseModel
from .product import Product
from typing import List

class RecommendResponse(BaseModel):
    recommendations: List[Product]