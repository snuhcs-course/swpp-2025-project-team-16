from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.shortcuts import get_object_or_404
from .models import PoseAnalysis
from .serializers import PoseAnalysisSerializer
import json, os, subprocess, tempfile
from pathlib import Path
from django.conf import settings

# -----------------------------------
# Helper: 포즈 분석 subprocess 실행
# -----------------------------------
def run_pose_analysis(payload: dict):
    conda_exe   = getattr(settings, "CONDA_EXE", "conda")
    env_name    = getattr(settings, "POSE_ENV", "")
    python_path = getattr(settings, "POSE_PYTHON_PATH", "")
    entry       = getattr(settings, "POSE_ENTRY", "")
    timeout_s   = int(getattr(settings, "POSE_TIMEOUT", 180))

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
            return {"error": f"Evaluation timed out after {timeout_s}s."}, 504

        if proc.returncode != 0:
            return {
                "error": "External evaluation failed.",
                "stderr": proc.stderr[-4000:],
                "stdout_tail": proc.stdout[-1000:],
            }, 500

        if out_path.exists():
            try:
                data = json.loads(out_path.read_text(encoding="utf-8"))
                return data, 200
            except Exception:
                raw = out_path.read_text(encoding="utf-8", errors="replace")
                return {"error": "Result file is not valid JSON.", "raw_head": raw[:1000]}, 500

        # fallback: stdout 마지막 JSON 시도
        try:
            data = _extract_last_json(proc.stdout)
            return data, 200
        except Exception:
            return {
                "error": "Result file not found and stdout has no valid JSON.",
                "stdout_tail": proc.stdout[-1000:],
            }, 500


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
def evalute_posture(request):
    user = request.user
    try:
        payload = request.data
    except Exception as e:
        return Response({"error": f"Invalid JSON: {e}"}, status=status.HTTP_400_BAD_REQUEST)

    result, code = run_pose_analysis(payload)
    if code != 200:
        return Response(result, status=code)

    pose = PoseAnalysis.objects.create(
        user=user,
        session_id=payload.get("session_id"),
        schedule_id=payload.get("schedule_id"),
        image_url=payload.get("image_url"),
        pose_data=result.get("pose_data"),
        ai_comment=result.get("ai_comment"),
    )
    serializer = PoseAnalysisSerializer(pose)
    return Response(serializer.data, status=status.HTTP_201_CREATED)

# -----------------------------------
# PoseAnalysis 전체 조회
# -----------------------------------
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def pose_analyses_view(request):
    poses = PoseAnalysis.objects.filter(user=request.user).order_by("-created_at")
    serializer = PoseAnalysisSerializer(poses, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

# -----------------------------------
# PoseAnalysis 상세 조회
# -----------------------------------
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def pose_analysis_detail(request, id):
    pose = get_object_or_404(PoseAnalysis, id=id, user=request.user)
    serializer = PoseAnalysisSerializer(pose)
    return Response(serializer.data, status=status.HTTP_200_OK)

# -----------------------------------
# Session 기반 PoseAnalysis 조회
# -----------------------------------
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def pose_analyses_by_session(request, session_id):
    poses = PoseAnalysis.objects.filter(user=request.user, session_id=session_id).order_by("-created_at")
    serializer = PoseAnalysisSerializer(poses, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

# -----------------------------------
# Schedule 기반 PoseAnalysis 조회
# -----------------------------------
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def pose_analyses_by_schedule(request, schedule_id):
    poses = PoseAnalysis.objects.filter(user=request.user, schedule_id=schedule_id).order_by("-created_at")
    serializer = PoseAnalysisSerializer(poses, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)
