from sentence_transformers import SentenceTransformer
import numpy as np

# 사전 학습된 모델 로드 (추천: all-MiniLM-L6-v2)
# 이 모델은 속도와 성능 균형이 좋아요.

class Vectorizer:
    def __init__(self, model_name="sentence-transformers/all-MiniLM-L6-v2"):
        self.model = SentenceTransformer(model_name)

    def encode(self, text: str) -> np.ndarray:
        if not text:
            return np.zeros(self.model.get_sentence_embedding_dimension())
        return self.model.encode(text, normalize_embeddings=True)

    def batch_encode(self, texts: list[str]) -> np.ndarray:
        return self.model.encode(texts, normalize_embeddings=True)
