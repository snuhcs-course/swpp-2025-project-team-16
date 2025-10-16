from fastapi import FastAPI, HTTPException
from typing import List

from schemas.recommend_request import RecommendRequest
from schemas.product import Product
from schemas.recommend_response import RecommendResponse

from services.recommendation import Recommendation
from db.vector_db import VectorDB
from utils.embedding import Vectorizer
from utils.level_extractor import LevelExtractor
from utils.product_loader import load_product_map

app = FastAPI(title="Equipment Recommendation API")

vectorizer = Vectorizer(model_name="sentence-transformers/all-MiniLM-L6-v2")
level_extractor = LevelExtractor(vectorizer=vectorizer)
vector_db = VectorDB(vectorizer=vectorizer)
recommender = Recommendation(vectorizer=vectorizer, vector_db=vector_db)

PRODUCT_MAP = load_product_map()

vector_db.build(PRODUCT_MAP, level_extractor=level_extractor)

print(vector_db.db)

@app.post("/recommend", response_model=RecommendResponse)
async def recommend(req: RecommendRequest):
    try:
        vec_results = recommender.get_recommendations(
            top_k=req.top_k or 5,
            equipment_name=req.equipment,
            user_level=req.user_level,
            requirements=req.requirements
        )

        results = []

        for v in vec_results:
            product_id = v["product_id"]
            assert product_id in PRODUCT_MAP, f"Unexpected product_id {product_id} not found in PRODUCT_MAP"
            product_meta = PRODUCT_MAP[product_id]
            results.append(product_meta)

        return {"recommendations": results}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
async def root():
    return {"message": "Recommendation API is running"}

