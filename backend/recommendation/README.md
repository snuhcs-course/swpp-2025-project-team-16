# Recommendation Backend

## 개요
이 서비스는 사용자가 입력한 **운동 장비와 사용자 수준**, 그리고 선택적으로 **사용자 요구사항**을 기반으로 적합한 제품을 추천합니다.  

- **입력:** 운동 용품, 사용자 수준, (옵션) 사용자 요구사항  
- **출력:** 운동 용품 리스트 (제품 정보 포함, 앱 UI 컴포넌트로 바로 활용 가능)  
- **준비물:** 전처리된 데이터셋

추천 로직은 다음 과정을 거칩니다:

1. 전처리된 제품 데이터셋 로드
2. 벡터 DB 저장 (제품 설명 → 임베딩)
2. 벡터 DB 검색 (선택 장비, 사용자 수준) → (선택적) 요구사항 기반 필터링
4. 최종 후보군 JSON 반환  

---

## 벡터 DB
- 생성: 자연어로 설명된 상품 정보로부터 수준 추출. 상품 정보는 임베딩 모델로 벡터화.
- 구조: 제품 ID + 장비 ID + 수준 + 벡터
- 기능: 장비 ID, 수준으로 검색 가능
- 활용: 자연어 입력을 벡터화시켜 벡터화된 제품 정보와 유사도 계산해서 추천

## 추천 처리 흐름
flowchart TD
    A[사용자 입력: 장비 종류 + 수준] --> B[벡터 DB 검색<br>WHERE equipment_id = 선택 장비, level = 사용자 수준]
    B --> C[후보군 생성<br>(장비 ID + 제품 ID)<br>제품 정보 JSON 반환, 벡터 포함]
    C --> D{사용자 요구사항 입력?}
    D -- Yes --> E[선택된 후보군 벡터와<br>사용자 요구사항 벡터 유사도 계산]
    D -- No --> F[후보군 그대로 UI JSON 반환]
    E --> F


```
사용자 입력: 장비 종류 + 수준
       │
       ▼
벡터 DB 검색: WHERE equipment_id = 선택 장비, level = 사용자 수준
       │
       ▼
후보군의 `장비 ID + 제품 ID` → 제품 정보 JSON 반환 (벡터 포함) ◀ ──────
                                       │                          |
                                       │ if  사용자 요구사항        |
                                       |                          |
                                       ▼                          |
                        사용자 요구사항 벡터화 + 유사도 계산       ────
```

## (추가 필요) 캐싱
백엔드는 앱으로 보낸 후보군 리스트를 기억해서  
사용자 요구사항이 추가로 왔을 때 재필터링해서 리스트 반환  

```mermaid
flowchart TD
    A[장비 선택] -->|Step 1: 후보군 생성| B[제품 후보군 JSON 반환 & 서버 캐시]
    B --> C[사용자 수준 입력] 
    C -->|Step 2: 제품 후보군 내 수준 필터링| D[수준 후보군 JSON 반환 & 서버 캐시]
    D --> E[요구사항 입력]
    E -->|Step 3: 벡터 기반 유사도 계산| F[요구사항 필터링된 JSON 반환]

    subgraph "스테이트 변화 규칙"
        G[요구사항만 변경] -->|Step 3 재계산| F
        H[수준 변경] -->|Step 2 재계산 + Step 3 삭제| D
        I[제품 변경] -->|Step 1 재계산 + Step 2,3 삭제| B
    end


(추가 필요) 제품 설명과 사용자 수준, 요구사항에 맞춰 제품 추천 이유 제공  

```
backend/
└── recommendation/
    ├── data/
    │   └── raw_products.py
    ├── db/
    │   └── vector_db.py
    ├── schemas/
    │   ├── recommendation_request.py       # 추천 요청 구조체 (pydantic)
    │   ├── recommendation_response.py      # 추천 응답 구조체 (pydantic)
    │   └── product.py                      # 상품 정보 구조체 (pydantic)
    ├── scripts/
    │   └── preprocess_products.py
    ├── services/
    │   └── recommendation.py
    ├── utils/
    │   ├── config.py                       # 환경설정 (나중에 공유 DB 전환 가능)
    │   ├── cosine.py
    │   ├── embedding.py
    │   ├── equipment_mapper.py
    │   ├── level_extractor.py
    |   ├── product_loader.py
    |   └── string_utils.py
    ├── main.py                             # FastAPI 엔트리 포인트
    ├── README.md
    └── requirements.txt

## 테스트
1. `pip install -r requirements.txt`
2. scripts/preprocess_products.py 실행 → data/preprocessed_products.json 생성
3. FastAPI 서버 실행
```
cd backend/recommendation
uvicorn main:app --host 0.0.0.0 --port 8000
```
4. curl로 API 테스트
- POST 요청
```
curl -X POST "http://127.0.0.1:8000/recommend" \
-H "Content-Type: application/json" \
-d '{
  "user_id": "user123",
  "equipment": "요가매트",
  "user_level": 2,
  "requirements": "Non-slip and durable",
  "top_k": 5
}'
```
- GET 요청
```
curl http://127.0.0.1:8000/
```

## 추가 사항
- 벡터 DB 구축, 클라이언트
- 전처리 상품 DB (?)
- 임베딩 모델 성능 개선
- 외부 서버 연결 확인
- 앱과 연결

- 널값, 0값 처리
- 데이터셋 구축