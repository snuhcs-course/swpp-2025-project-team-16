from fastapi import FastAPI, HTTPException
from models.inference import pose_evaluation_pipeline
from pydantic import BaseModel
from models.vlm_service import VLMService  # ✅ 기존 posture evaluator import
import uvicorn
import traceback

# FastAPI app 생성
app = FastAPI(title="VLM Posture Evaluation API", version="1.0")

# VLM 서비스 초기화
vlm = VLMService()


# 요청 body schema 정의
class ImageRequest(BaseModel):
    image_base64: str
    category: str


@app.post("/evaluate_posture")
async def evaluate_posture(req: ImageRequest):
    """
    Base64 이미지와 설명 텍스트를 받아 자세 평가 결과(JSON) 반환
    """
    try:
        # VLM 모델 실행
        result = pose_evaluation_pipeline(vlm, req.image_base64, req.category)
        print(result)
        # 문자열 형태 JSON으로 반환
        return {
            "status": "success",
            "good_points": result["good_points"],
            "improvement_points": result["improvement_points"],
            "improvement_methods": result["improvement_methods"]
        }

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # GPU 서버에서 실행 시
    uvicorn.run(app, host="0.0.0.0", port=8080)
