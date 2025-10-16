import sys
from pathlib import Path
import json

BASE_DIR = Path(__file__).resolve().parent.parent  # scripts/ 기준 상위 1단계
sys.path.append(str(BASE_DIR))

from utils.string_utils import normalize_string

RAW_DATA_PATH = BASE_DIR / "data/raw_products.json"
OUTPUT_PATH = BASE_DIR / "data/preprocessed_products.json"

def preprocess_product(raw_product):
    product_id = raw_product.get("id")
    if not product_id:
        return None

    equipment_name = raw_product.get("category")
    equipment_name = normalize_string(equipment_name)

    description = raw_product.get("description") or ""
    if isinstance(description, list):
        description = " ".join(description).lower()
    elif not isinstance(description, str):
        description = str(description)

    return {
        "product_id": str(product_id),
        "equipment_name": equipment_name,
        "description": description.lower(),
        "name": raw_product.get("name"),
        "price": raw_product.get("price"),
        "image_urls": raw_product.get("image_urls"),
        "brand": raw_product.get("brand"),
        "stock": raw_product.get("stock"),
        "rating": raw_product.get("rating")
    }

def preprocess_products(raw_products):
    preprocessed = []
    for product in raw_products:
        processed = preprocess_product(product)
        if processed:
            preprocessed.append(processed)
    return preprocessed



if __name__ == "__main__":
    with open(RAW_DATA_PATH, "r", encoding="utf-8") as f:
        raw_products = json.load(f)
    
    product_map = preprocess_products(raw_products)

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(product_map, f, ensure_ascii=False, indent=2)
    
    print(f"Preprocessed product_map saved to {OUTPUT_PATH}")