import argparse
import sys, os, json, traceback, tempfile
from typing import Optional, Union, Dict, Any, List
from pydantic import BaseModel, Field, ValidationError
from dotenv import load_dotenv

# ==== 프로젝트 모듈 ====
from pose_vlm.models.vlm_service import VLMService
from pose_vlm.models.utils import imagepath_to_base64
from pose_vlm.models.inference import pose_evaluation_pipeline

import cv2

# ======== 입력 스키마 ========
class EvalRequest(BaseModel):
    image_base64: Optional[str] = None      # 기본: base64
    image_path: Optional[str] = None        # 테스트 대체 입력(선택)
    category: str = Field(..., description="e.xg., 'squat'")


# ======== 출력 도우미 ========
def atomic_write_text(path: str, text: str, encoding="utf-8"):
    """ tmp에 쓰고 rename으로 교체 → 원자적 쓰기 """
    dirpath = os.path.dirname(path) or "."
    fd, tmppath = tempfile.mkstemp(prefix=".tmp_", dir=dirpath)
    try:
        with os.fdopen(fd, "w", encoding=encoding) as f:
            f.write(text)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmppath, path)
    finally:
        try:
            if os.path.exists(tmppath):
                os.remove(tmppath)
        except Exception:
            pass

def _emit_json(obj: Dict[str, Any], out_path: Optional[str]):
    text = json.dumps(obj, ensure_ascii=False)
    if out_path:
        atomic_write_text(out_path, text)
    else:
        # 가능하면 사용하지 않지만, 호환을 위해 유지
        print(text)

# ======== 메인 ========
def main():
    load_dotenv()  # API 키 등 로드
    ap = argparse.ArgumentParser()
    ap.add_argument("--stdin", action="store_true", help="Read request JSON from stdin")
    ap.add_argument("--infile", type=str, help="Read request JSON from a file path")
    ap.add_argument("--out", type=str, help="Write response JSON to this file instead of stdout")
    args = ap.parse_args()

    # 입력 읽기 & 검증
    try:
        if args.stdin:
            raw = sys.stdin.read()
        elif args.infile:
            with open(args.infile, "r", encoding="utf-8") as f:
                raw = f.read()
        else:
            print("Provide --stdin or --infile", file=sys.stderr)
            _emit_json({"ok": False, "error": "No input provided"}, args.out)
            sys.exit(2)

        payload = json.loads(raw)
        req = EvalRequest(**payload)
    except ValidationError as ve:
        print(ve.json(), file=sys.stderr)
        _emit_json({"ok": False, "error": "Invalid request schema", "details": json.loads(ve.json())}, args.out)
        sys.exit(1)
    except Exception as e:
        print(traceback.format_exc(), file=sys.stderr)
        _emit_json({"ok": False, "error": f"Bad JSON: {e}"}, args.out)
        sys.exit(1)

    # image_path → image_base64 대체 허용
    if not req.image_base64 and req.image_path:
        try:
            req.image_base64 = imagepath_to_base64(req.image_path)
        except Exception as e:
            print(traceback.format_exc(), file=sys.stderr)
            _emit_json({"ok": False, "error": f"Failed to read image_path: {e}"}, args.out)
            sys.exit(1)

    if not req.image_base64:
        _emit_json({"ok": False, "error": "image_base64 is required (or provide image_path)."}, args.out)
        sys.exit(1)

    # 추론 실행
    try:
        vlm = VLMService()
        result_summary = pose_evaluation_pipeline(vlm, req.image_base64, category=req.category)

        # 호환: dict이면 통일된 키 사용, 문자열이면 summary로 래핑
        resp: Dict[str, Any]
        if isinstance(result_summary, dict):
            # 가능한 키들 정규화
            good = result_summary.get("good_points") or result_summary.get("good") or []
            bad  = result_summary.get("improvement_points") or result_summary.get("bad") or []
            meth = result_summary.get("improvement_methods") or result_summary.get("methods") or []
            # # 문자열일 수도 있으니 안전 처리
            # if isinstance(good, str): good = [good]
            # if isinstance(bad, str):  bad = [bad]
            # if isinstance(meth, str): meth = [meth]

            resp = {
                "status": "success",
                "good_points": good,
                "improvement_points": bad,
                "improvement_methods": meth,
            }
            # # 추가 필드가 있으면 그대로 보존
            # for k, v in result_summary.items():
            #     if k not in resp:
            #         resp[k] = v
        else:
            resp = {
                "status": "success",
                "summary": str(result_summary)
            }

        _emit_json(resp, args.out)
    except Exception as e:
        print(traceback.format_exc(), file=sys.stderr)
        _emit_json({"ok": False, "error": str(e)}, args.out)
        sys.exit(1)

if __name__ == "__main__":
    main()
