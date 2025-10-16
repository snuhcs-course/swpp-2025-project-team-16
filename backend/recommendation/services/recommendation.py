from db.vector_db import VectorDB
from utils.embedding import Vectorizer
from utils.equipment_mapper import get_equipment_id

class Recommendation:
    def __init__(self, vector_db: VectorDB, vectorizer: Vectorizer = None):
        self.vectorizer = vectorizer or Vectorizer()
        self.vector_db = vector_db

    def get_recommendations(self, equipment_name, user_level, top_k=-1, requirements=None):
        equipment_id = get_equipment_id(equipment_name)

        if equipment_id is None:
            return []

        candidates = self.vector_db.search(
            top_k=top_k if not requirements else -1,
            equipment_id=equipment_id,
            level=user_level
        )

        if requirements:
            candidates = self.get_recommendations_with_requirements(candidates, requirements, top_k)
        
        return candidates
    
    def get_recommendations_with_requirements(self, products, requirements, top_k=5):
        req_vec = self.vectorizer.encode(requirements)
        scored = [(p, cosine_similarity(p["vector"], req_vec)) for p in products]
        scored.sort(key=lambda x: x[1], reverse=True)
        return [p for p, _ in scored[:top_k]]