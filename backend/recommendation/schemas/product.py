from pydantic import BaseModel
from typing import Optional, List

class Product(BaseModel):
    product_id: str
    equipment_name: str
    description: str
    name: str
    price: Optional[float]
    image_urls: List[str]
    brand: Optional[str]
    stock: Optional[int]
    rating: Optional[float]