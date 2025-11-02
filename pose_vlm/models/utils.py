import base64
from math import atan2
import math
import os
import numpy as np
import cv2

def imagepath_to_base64(image_path: str) -> str:
    """
    Convert an image file (e.g., .jpg, .png) to a base64 string.

    Args:
        image_path (str): Path to the image file.

    Returns:
        str: Base64 encoded string (UTF-8).
    """
    if not os.path.exists(image_path):
        raise FileNotFoundError(f"Image file not found: {image_path}")

    with open(image_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")
    
def imagecv2_to_base64(image_cv2: np.ndarray, ext: str = '.jpg') -> str:
    """
    Convert an OpenCV image (numpy array) to a base64-encoded string.

    Args:
        image_cv2 (np.ndarray): The image loaded or processed by OpenCV (BGR format).
        ext (str): The desired image format extension (e.g., '.jpg', '.png').

    Returns:
        str: Base64 encoded string (UTF-8).
    """
    if image_cv2 is None:
        raise ValueError("Input image is None. Please provide a valid cv2 image array.")

    # Encode image to memory buffer (e.g., JPEG or PNG format)
    success, buffer = cv2.imencode(ext, image_cv2)
    if not success:
        raise ValueError("Failed to encode image using cv2.imencode().")

    # Convert bytes to base64 string
    base64_str = base64.b64encode(buffer).decode('utf-8')
    return base64_str

def base64_to_cv2(image_base64: str):
    """
    Convert a base64-encoded image (JPEG/PNG/etc) into an OpenCV image
    that is equivalent to cv2.imread(): uint8 HxWx3 (BGR) or HxW (grayscale).
    """
    print(image_base64[:30])  # 디버그용 출력
    # 1) base64 앞에 'data:image/jpeg;base64,...' 같은 prefix가 붙어 있을 수도 있으니 제거
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    # 2) base64 문자열 → raw bytes
    img_bytes = base64.b64decode(image_base64)

    # 3) bytes → numpy 1D uint8 array
    img_array = np.frombuffer(img_bytes, dtype=np.uint8)

    # 4) OpenCV 디코딩 (cv2.imdecode = 메모리 상의 파일을 cv2.imread처럼 읽음)
    img = cv2.imdecode(img_array, cv2.IMREAD_COLOR)
    # IMREAD_COLOR => cv2.imread(..., cv2.IMREAD_COLOR)와 동일하게
    # - 3채널 BGR
    # - 알파 채널 버림

    return img

def angle_between(v1, v2):
    """벡터 v1, v2 사이 각도 (라디안 단위)"""
    v1_u = v1 / np.linalg.norm(v1)
    v2_u = v2 / np.linalg.norm(v2)
    cos_angle = np.clip(np.dot(v1_u, v2_u), -1.0, 1.0)
    return np.degrees(np.arccos(cos_angle))


def compute_joint_angles(keypoints_3d, keypoints_2d):
    """입력: (N,2) keypoints_3d, 출력: 주요 관절 각도 dict"""
    kp3d = keypoints_3d
    kp2d = keypoints_2d

    angles = {}
    def ang(a, b, c):
        return angle_between(kp3d[a] - kp3d[b], kp3d[c] - kp3d[b])

    angles = {}

    # 무릎
    angles['left_knee'] = ang(11, 13, 15)
    angles['right_knee'] = ang(12, 14, 16)

    # # 발목
    # angles['left_ankle'] = ang(13, 15, 17)
    # angles['right_ankle'] = ang(14, 16, 22)

    # 팔꿈치
    angles['left_elbow'] = ang(5, 7, 9)
    angles['right_elbow'] = ang(6, 8, 10)

    # 허리 (몸통 기울기)    
    angles['left_waist']  = ang(5, 11, 13)   # shoulder_L - hip_L - knee_L
    angles['right_waist'] = ang(6, 12, 14)  # hip–shoulder–nose

    angles["left_ankle"] = angle_between(np.array([1, 0]), kp2d[19] - kp2d[17])
    angles["right_ankle"] = angle_between(np.array([1, 0]),kp2d[22] - kp2d[20] )
    return angles

def draw_keypoints(image, keypoints_2d, skeleton, save_img = False):
    # img_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    img_bgr = image.copy()
    body_points = keypoints_2d[0:23]    

    for (i, j) in skeleton:
        pt1, pt2 = body_points[i], body_points[j]
        cv2.line(img_bgr, (int(pt1[0]), int(pt1[1])), (int(pt2[0]), int(pt2[1])), (0, 255, 0), 2)

    for i in range(keypoints_2d.shape[0]):
        if i >= 0 and i <= 22:
            x, y = keypoints_2d[i]
            cv2.circle(img_bgr, (int(x), int(y)), 3, (255, 0, 0), -1)  # 빨간 점

    if save_img:
        cv2.imwrite('output_skeleton.png', img_bgr)

    return img_bgr