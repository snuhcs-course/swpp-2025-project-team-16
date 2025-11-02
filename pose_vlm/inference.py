from pydantic import BaseModel, Field
from dotenv import load_dotenv
from models.vlm_service import VLMService
from models.utils import imagepath_to_base64, compute_joint_angles, imagecv2_to_base64
from models.skeleton import pose_model_inference
from models.instructions import squat_instruction
from models.human import rule_based_pose_evaluation

import cv2
import matplotlib.pyplot as plt
from rtmpose3d import RTMPose3D


import time


start_time = time.time()


vlm = VLMService()

image_path = "sample_data/push_up3.png"

image = cv2.imread(image_path)

img_skeleton, joint_angles, keypoints_2d = pose_model_inference(image, save_img = True)
base64_image = imagecv2_to_base64(img_skeleton, ext='.jpg')
asdfadsfs
human_opinion = rule_based_pose_evaluation(joint_angles, keypoints_2d, category="squat")
print(human_opinion)

result = vlm.evaluate_posture(base64_image, squat_instruction, human_opinion)
print(result)
end_time = time.time()

answer = result["feedback"]

print(vlm.evaluate_score(squat_instruction, answer))

print(f"Inference time: {end_time - start_time} seconds")

