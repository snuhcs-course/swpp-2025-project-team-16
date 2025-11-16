# pose/views.py
import json, os, subprocess, tempfile
from pathlib import Path
from django.http import JsonResponse, HttpResponseBadRequest
from django.views.decorators.http import require_POST
from django.views.decorators.csrf import csrf_exempt
from django.conf import settings

@csrf_exempt
@require_POST
def evaluate_posture(request):
    # 1) 입력 검증
    try:
        payload_text = request.body.decode("utf-8")
        json.loads(payload_text)
    except Exception as e:
        return HttpResponseBadRequest(f"Invalid JSON: {e}")

    # 2) 설정
    conda_exe   = getattr(settings, "CONDA_EXE", "conda")
    env_name    = getattr(settings, "POSE_ENV", "")                # 예: "vlm"
    python_path = getattr(settings, "POSE_PYTHON_PATH", "")        # 예: "/home/team16/swpp-2025-project-team-16"
    entry       = getattr(settings, "POSE_ENTRY", "")              # 예: "pose_vlm.inference"
    timeout_s   = int(getattr(settings, "POSE_TIMEOUT", 180))

    if not env_name or not entry:
        return JsonResponse({"error": "Server misconfigured: POSE_ENV/POSE_ENTRY missing."}, status=500)
    if not python_path:
        return JsonResponse({"error": "Server misconfigured: POSE_PYTHON_PATH missing."}, status=500)

    # 3) 환경변수 구성 (여기에 PYTHONPATH 넣기)
    env = os.environ.copy()
    env.setdefault("PYTHONUNBUFFERED", "1")
    # 프로젝트 루트가 sys.path에 들어가도록 설정
    # 기존
    env["PYTHONPATH"] = python_path + (os.pathsep + env["PYTHONPATH"] if "PYTHONPATH" in env else "")

    # 수정: rtmpose3d의 '부모' 경로도 추가
    extra_paths = [
        os.path.join(python_path, "pose_vlm", "rtmpose3d"),  # <- 중요: 부모 경로
    ]
    env["PYTHONPATH"] = os.pathsep.join(
        [python_path, *extra_paths, env.get("PYTHONPATH", "")]
)


    with tempfile.TemporaryDirectory(prefix="pose_eval_") as tdir:
        tdirp = Path(tdir)
        out_path = tdirp / "result.json"
        in_path  = tdirp / "request.json"

        # 요청 바디 그대로 임시 파일에 기록 (이미 위에서 유효성 검증 완료)
        in_path.write_text(payload_text, encoding="utf-8")

        # 모듈을 파일 입력 방식으로 실행 (STDIN 사용 안 함)
        cmd = [
            conda_exe, "run", "-n", env_name,
            "python", "-m", entry,
            "--infile", str(in_path),
            "--out", str(out_path),
        ]

        try:
            proc = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=timeout_s,
                env=env,
                text=True,  # stdout/stderr를 문자열로 받기
            )
        except subprocess.TimeoutExpired:
            return JsonResponse({"error": f"Evaluation timed out after {timeout_s}s."}, status=504)

        if proc.returncode != 0:
            return JsonResponse({
                "error": "External evaluation failed.",
                "stderr": proc.stderr[-4000:],
                "stdout_tail": proc.stdout[-1000:],
            }, status=500)

        if out_path.exists():
            try:
                data = json.loads(out_path.read_text(encoding="utf-8"))

                return JsonResponse(data, status=200)
            except Exception:
                raw = out_path.read_text(encoding="utf-8", errors="replace")
                return JsonResponse({"error": "Result file is not valid JSON.", "raw_head": raw[:1000]}, status=500)

        raw_out = proc.stdout
        try:
            data = _extract_last_json(raw_out)
            return JsonResponse(data, status=200)
        except Exception:
            return JsonResponse({
                "error": "Result file not found and stdout has no valid JSON.",
                "stdout_tail": raw_out[-1000:],
            }, status=500)


def _extract_last_json(text: str):
    import json
    dec = json.JSONDecoder()
    for i in range(len(text) - 1, -1, -1):
        if text[i] == "{":
            try:
                obj, _ = dec.raw_decode(text[i:])
                return obj
            except Exception:
                continue
    raise ValueError("No valid JSON object found")
