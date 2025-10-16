from utils.embedding import Vectorizer
import numpy as np

class LevelExtractor:
    def __init__(self, vectorizer: Vectorizer = None):
        self.vectorizer = vectorizer or Vectorizer()
        self.keyword_map = {
            1: ["가벼운", "초보", "쉬움", "입문"],
            2: ["중급", "강화", "균형", "근육"],
            3: ["고급", "무거운", "전문", "고난도"],
        }
    
    def extract(self, description: str) -> int:
        if not description:
            return 2

        desc = description.lower()

        for level, keywords in self.keyword_map.items():
            if any(k in desc for k in keywords):
                return level

        return self._vector_based_level(description)

    def _vector_based_level(self, description: str) -> int:
        vec = self.vectorizer.encode(description)
        score = np.mean(vec)
        if score < 0.3:
            return 1
        elif score < 0.6:
            return 2
        else:
            return 3
