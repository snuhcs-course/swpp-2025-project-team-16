#!/usr/bin/env python
import sys, os, json, argparse, traceback, tempfile

from typing import Optional, Union, Dict, Any, List
from pydantic import BaseModel, Field, ValidationError
from dotenv import load_dotenv

# ==== 프로젝트 모듈 ====
from pose_vlm.models.vlm_service import VLMService
from pose_vlm.models.utils import base64_to_cv2, imagepath_to_base64, imagecv2_to_base64
from pose_vlm.models.skeleton import pose_model_inference
from pose_vlm.models.instructions import squat_instruction, lunge_instruction, plank_instruction
from pose_vlm.models.human import rule_based_pose_evaluation
import cv2



# ======== 파이프라인 ========
def pose_evaluation_pipeline(vlm: VLMService, image_base64: str, category: str) -> Union[Dict[str, Any], str]:
    img_cv2 = base64_to_cv2(image_base64)
    cv2.imwrite("./debug_image.png", img_cv2)

    img_skeleton, joint_angles, keypoints_2d = pose_model_inference(img_cv2, save_img=True)
    cv2.imwrite("./debug_image.png", img_skeleton)

    img_skeleton_base64 = imagecv2_to_base64(img_skeleton, ext='.jpg')

    if joint_angles is None or keypoints_2d is None:
        human_opinion = ""
    else:
        human_opinion = rule_based_pose_evaluation(joint_angles, keypoints_2d, category=category)
    
    if category =="squat":
        instruction = squat_instruction 
    elif category == "lunge" :
        instruction = lunge_instruction
    elif category =="plank":
        instruction = plank_instruction
    else :
        instruction = ""

    result = vlm.evaluate_posture(img_skeleton_base64, instruction, human_opinion)
    # result["feedback"]가 문자열일 수 있음 → summarize
    result_summary = vlm.summarize(result.get("feedback", ""))

    # result_summary가 dict(권장) 또는 string일 수 있음. 그대로 반환.
    return result_summary
