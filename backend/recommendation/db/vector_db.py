from utils.embedding import Vectorizer
from utils.level_extractor import LevelExtractor
from utils.equipment_mapper import get_equipment_id

class VectorDB:
    """
    단순 in-memory 벡터DB
    (향후 Milvus, Pinecone 등으로 교체 가능)
    """
    def __init__(self, vectorizer: Vectorizer = None):
        self.vectorizer = vectorizer or Vectorizer()
        self.db = []

    def build(self, product_map: dict, level_extractor: LevelExtractor):
        self.db = []
        for product_id, product in product_map.items():
            desc = product.get("description", "")
            self.db.append({
                "product_id": product_id,
                "equipment_id": get_equipment_id(product.get("equipment_name")),
                "level": level_extractor.extract(desc),
                "vector": self.vectorizer.encode(desc)
            })

        print(f"VectorDB built: {len(self.db)} vectors")
    
    def search(self, top_k, products=None, **filters):
        result = products or self.db
        for key, value in filters.items():
            if value is not None:
                result = [p for p in result if p.get(key) == value]
        return result[:top_k]
