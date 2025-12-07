from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
import json, os, subprocess, tempfile, base64, uuid
from pathlib import Path
from django.conf import settings
from django.http import JsonResponse, HttpResponseBadRequest
from django.core.files.base import ContentFile
from django.utils import timezone

from .models import PoseAnalysis
from .serializers import PoseAnalysisSerializer


# -----------------------------------
# Helper: 포즈 분석 subprocess 실행
# -----------------------------------
def run_pose_analysis(payload: dict):
    conda_exe   = getattr(settings, "CONDA_EXE", "conda")
    env_name    = getattr(settings, "POSE_ENV", "")
    python_path = getattr(settings, "POSE_PYTHON_PATH", "")
    entry       = getattr(settings, "POSE_ENTRY", "")
    timeout_s   = int(getattr(settings, "POSE_TIMEOUT", 300))
    # print(payload)
    env = os.environ.copy()
    env.setdefault("PYTHONUNBUFFERED", "1")
    env["PYTHONPATH"] = python_path

    extra_paths = [os.path.join(python_path, "pose_vlm", "rtmpose3d")]
    env["PYTHONPATH"] = os.pathsep.join([python_path, *extra_paths, env.get("PYTHONPATH", "")])

    with tempfile.TemporaryDirectory(prefix="pose_eval_") as tdir:
        tdirp = Path(tdir)
        out_path = tdirp / "result.json"
        in_path  = tdirp / "request.json"
        in_path.write_text(json.dumps(payload), encoding="utf-8")

        cmd = [
            conda_exe, "run", "-n", env_name,
            "python", "-m", entry,
            "--infile", str(in_path),
            "--out", str(out_path),
        ]


        # error_log_path = "/home/team16/error.log"

        # with open(error_log_path, "w") as err_file:
        #     proc = subprocess.run(
        #         cmd,
        #         stdout=subprocess.PIPE,   # stdout은 메모리로
        #         stderr=err_file,          # stderr은 파일로! 🔥 핵심
        #         timeout=timeout_s,
        #         env=env,
        #         text=True,
        #     )

        try:
            proc = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=timeout_s,
                env=env,
                text=True,
            )
        except subprocess.TimeoutExpired:
            return JsonResponse({"error": f"Evaluation timed out after {timeout_s}s."}, status=504)

        if proc.returncode != 0:
            # print("STDOUT:")
            # print(proc.stdout)
            # print("STDERR:")
            # print(proc.stderr)
            # print("================================")
            return JsonResponse({
                "error": "External evaluation failed.",
                "stderr": proc.stderr[-4000:],
                "stdout_tail": proc.stdout[-1000:],
            }, status=500)

        if out_path.exists():
            try:
                data = json.loads(out_path.read_text(encoding="utf-8"))
                return data
            except Exception:
                raw = out_path.read_text(encoding="utf-8", errors="replace")
                return JsonResponse({"error": "Result file is not valid JSON.", "raw_head": raw[:1000]}, status=500)
        # fallback: stdout 마지막 JSON 시도
        try:
            data = _extract_last_json(proc.stdout)
            return data
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

# -----------------------------------
# PoseAnalysis Upload
# -----------------------------------
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def evaluate_posture(request):
    user = request.user
    data = request.data

    image_base64 = data.get("image_base64")
    category     = data.get("category")
    schedule_id  = data.get("schedule_id")
    session_id   = data.get("session_id")

    if not image_base64 or not category:
        return Response(
            {"error": "image_base64 and category are required."},
            status=status.HTTP_400_BAD_REQUEST)

    try:
        payload = {
            "image_base64": image_base64,
            "category": category
        }

        result = run_pose_analysis(payload)

        if "error" in result:
            return Response({"error": result["error"]}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


        pose_data = result.get("pose_data", {})
        summary   = result.get("summary", {})

        img_data = base64.b64decode(image_base64.split(",")[-1])
        filename = f"{uuid.uuid4().hex}_{timezone.now().strftime('%Y%m%d%H%M%S')}.jpg"
        file_path = os.path.join(settings.MEDIA_ROOT, "pose_images", filename)
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, "wb") as f:
            f.write(img_data)

        image_url = request.build_absolute_uri(settings.MEDIA_URL + "pose_images/" + filename)
        # image_url = os.path.join(settings.MEDIA_URL, "pose_images", filename)

        pose_analysis = PoseAnalysis.objects.create(
            user=user,
            schedule_id=schedule_id,
            session_id=session_id,
            activity=category,
            image_url=image_url,
            pose_data=pose_data,
            ai_comment=summary
        )

        serializer = PoseAnalysisSerializer(pose_analysis)
        serializer_data = serializer.data
        serializer_data_ai_comment = serializer_data.get("ai_comment", {})

        response_data = {
            "id": serializer_data.get("id"),
            "good_points": serializer_data_ai_comment.get("good_points"),
            "improvement_points": serializer_data_ai_comment.get("improvement_points"),
            "improvement_methods": serializer_data_ai_comment.get("improvement_methods")
        }

        return Response(response_data, status=status.HTTP_201_CREATED)

    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)