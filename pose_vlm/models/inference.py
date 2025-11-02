from pydantic import BaseModel, Field
from dotenv import load_dotenv
from models.vlm_service import VLMService
from models.utils import base64_to_cv2, imagepath_to_base64, compute_joint_angles, imagecv2_to_base64
from models.skeleton import pose_model_inference
from models.instructions import squat_instruction
from models.human import rule_based_pose_evaluation


import cv2
import matplotlib.pyplot as plt
from rtmpose3d import RTMPose3D



def pose_evaluation_pipeline(vlm: VLMService, image_base64: str, category: str):
    img_cv2 = base64_to_cv2(image_base64)
    cv2.imwrite("./debug_image.png", img_cv2)

    img_skeleton, joint_angles, keypoints_2d = pose_model_inference(img_cv2, save_img = True)
    img_skeleton_base64 = imagecv2_to_base64(img_skeleton, ext='.jpg')

    human_opinion = rule_based_pose_evaluation(joint_angles, keypoints_2d, category=category)
    if category == "squat":
        instruction = squat_instruction
    else:
        instruction = ""
    
    result = vlm.evaluate_posture(img_skeleton_base64, instruction, human_opinion)

    result_summary = vlm.summarize(result["feedback"])

    return result_summary