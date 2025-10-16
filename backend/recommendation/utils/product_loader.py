import json
from pathlib import Path
from functools import lru_cache

PRODUCT_JSON_PATH = Path(__file__).parent.parent / "data/preprocessed_products.json"

@lru_cache(maxsize=1)
def load_product_map():
    with open(PRODUCT_JSON_PATH, "r", encoding="utf-8") as f:
        products = json.load(f)
    
    return {product["product_id"]: product for product in products}



# If shopping mall API is available, this will be replaced with a real-time call.