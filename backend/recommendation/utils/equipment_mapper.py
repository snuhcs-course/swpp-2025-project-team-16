from .string_utils import normalize_string

exercise_map = {
    "골프": 1,
    "볼링": 2,
    "당구": 3,
    "필라테스": 4,
    "요가": 5,
}

equipment_map = {
    "덤벨": 1,
    "헬스볼": 2,
    "골프채": 3,
    "볼링공": 4,
    "당구채": 5,
    "필라테스 매트": 6,
    "요가매트": 7,
}

def get_equipment_id(name: str) -> int | None:
    normalized = normalize_string(name)
    for k, v in equipment_map.items():
        if normalize_string(k) == normalized:
            return v
    return None

# (확장) 제품 자체로 장비 분류