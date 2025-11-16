
import cv2
import matplotlib.pyplot as plt
# from pose_vlm.rtmpose3d import RTMPose3D
from rtmpose3d import RTMPose3D

from pose_vlm.models.utils import compute_joint_angles, draw_keypoints


skeleton = [
    [5, 7], [7, 9],         # Left arm
    [6, 8], [8, 10],        # Right arm
    [5, 6],                 # Shoulders
    [5, 11], [6, 12],       # Torso
    [11, 12],               # Hips
    [11, 13], [13, 15],     # Left leg
    [12, 14], [14, 16],     # Right leg
    [0, 1], [0, 2],         # Nose to eyes
    [1, 3], [2, 4],         # Eyes to ears
    [0, 5], [0, 6],          # Nose to shoulders
    [17, 19], [19, 15],
    [20, 22], [22, 16]      # Eyes to ears lower
]

# image = cv2.imread('sample_data/squat_wrong_1.png')
def pose_model_inference(image, save_img = False):
    # Initialize model (auto-downloads checkpoints from this repo)
    model = RTMPose3D.from_pretrained('rbarac/rtmpose3d', device='cuda:0')
    # Run inference
    results = model(image, return_tensors='np')

    if len(results['keypoints_2d'] > 0):
        # Access results
        keypoints_2d = results['keypoints_2d'].squeeze() # [N, 133, 2] - pixel coords
        keypoints_3d = results['keypoints_3d'].squeeze() # [N, 133, 3] - 3D coords in meters
        # scores = results['scores']          

        # compute joint angles
        angles = compute_joint_angles(keypoints_3d, keypoints_2d)

        # print(angles)

        img_skeleton = draw_keypoints(image, keypoints_2d, skeleton, save_img)


        return img_skeleton, angles, keypoints_2d
    
    else:
        return image, None, None




# joint_indices = {
#     'left_knee': 13,
#     'right_knee': 14,
#     'left_elbow': 7,
#     'right_elbow': 8,
#     'left_waist': 11,
#     'right_waist': 12,
# }

# for name, angle in angles.items():
#     if name in joint_indices:
#         x, y = points[joint_indices[name]]
#         cv2.putText(img_rgb, f"{angle:.1f}", (int(x) + 8, int(y) - 8),
#                     cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 0), 2, cv2.LINE_AA)



