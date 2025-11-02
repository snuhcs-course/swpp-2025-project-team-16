import numpy as np


def rule_based_pose_evaluation(joint_angles, keypoints_2d, category = "squat"):
    human_opinion = []
    if category == "squat":
        knee_angle = (joint_angles["left_knee"] + joint_angles["right_knee"]) / 2.0
        waist_angle = (joint_angles["left_waist"] + joint_angles["right_waist"]) / 2.0
        ankle_angle = (joint_angles["left_ankle"] + joint_angles["right_ankle"]) / 2.0

        if 20 <= waist_angle < 45:
            human_opinion.append("The torso appears to lean forward excessively.")
        elif 45 <= waist_angle < 60:
            human_opinion.append("The torso angle looks well-balanced and stable.")
        elif 60 <= waist_angle < 90:
            human_opinion.append("The torso seems too upright, with a risk of overextension.")
        else:
            a = a + 1
        # ---- Ankle angle evaluation ----
        if -5 <= ankle_angle < 5:
            human_opinion.append("The feet look fully grounded with even pressure distribution.")
        elif 5 <= ankle_angle < 10:
            human_opinion.append("The heels appear slightly lifted off the ground.")

        # 15 - 13 - 11, 16 - 14 - 12

        left_knee_ratio = (keypoints_2d[11][1] - keypoints_2d[13][1]) / (keypoints_2d[15][1] - keypoints_2d[13][1]) 
        right_knee_ratio = (keypoints_2d[12][1] - keypoints_2d[14][1]) / (keypoints_2d[16][1] - keypoints_2d[14][1])
        avg_knee_ratio = (left_knee_ratio + right_knee_ratio) / 2.0

        if -0.05 <= avg_knee_ratio < 0.1:
            human_opinion.append("The squat depth appears appropriate and controlled.")
        elif avg_knee_ratio >= 0.1:
            human_opinion.append("The squat seems quite deep, showing good mobility and stability.")
        else:
            human_opinion.append("The squat depth looks shallow, indicating limited hip flexion.")

    return " ".join(human_opinion)