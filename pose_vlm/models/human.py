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
            a = 1
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
    
    elif category == "lunge":

        # Front knee angle
        front_knee_angle = (joint_angles["left_knee"] + joint_angles["right_knee"]) / 2.0

        if 85 <= front_knee_angle <= 105:
            human_opinion.append("The front knee angle looks appropriate and stable for a lunge.")
        elif front_knee_angle < 85:
            human_opinion.append("The lunge depth appears shallow, limiting hip and quad engagement.")
        else:
            human_opinion.append("The front knee travels too far forward, which may increase joint stress.")

        # Torso lean
        waist_angle = (joint_angles["left_waist"] + joint_angles["right_waist"]) / 2.0

        if 40 <= waist_angle <= 60:
            human_opinion.append("The torso angle looks balanced and upright.")
        elif waist_angle < 40:
            human_opinion.append("The torso seems overly upright, which may restrict proper hip mechanics.")
        else:
            human_opinion.append("The torso leans forward excessively, reducing stability.")

        # Back knee depth ratio: using right leg as back leg
        back_ratio = (keypoints_2d[12][1] - keypoints_2d[14][1]) / (keypoints_2d[16][1] - keypoints_2d[14][1])

        if back_ratio < 0.15:
            human_opinion.append("The back knee drops vertically with good control.")
        elif 0.15 <= back_ratio <= 0.35:
            human_opinion.append("The back knee height looks reasonable for a stable lunge.")
        else:
            human_opinion.append("The back knee appears too high, reducing depth and stability.")


    elif category == "plank":
        # elbow_angle = (joint_angles["left_elbow"] + joint_angles["right_elbow"]) / 2.0
        # hip_angle = (joint_angles["left_hip"] + joint_angles["right_hip"]) / 2.0
        shoulder_x = keypoints_2d[5][0]   # Left shoulder
        ankle_x    = keypoints_2d[16][0]  # Right ankle

        # 방향 판단
        if shoulder_x < ankle_x:
            # 사람이 왼쪽을 보고 있으므로, 왼팔이 카메라 쪽
            elbow_angle = joint_angles["left_elbow"]
        else:
            # 사람이 오른쪽을 보고 있으므로, 오른팔이 카메라 쪽
            elbow_angle = joint_angles["right_elbow"]

        
        waist_angle = (joint_angles["left_waist"] + joint_angles["right_waist"]) / 2.0

        if 160 <= waist_angle <= 180:
            human_opinion.append("The spine looks straight and neutral.")
        elif waist_angle < 160:
            human_opinion.append("The hips appear to sag, indicating reduced core engagement.")
        else:
            human_opinion.append("The hips seem too high, reducing core activation.")

        if 80 <= elbow_angle <= 100:
            human_opinion.append("Your elbows are well aligned under your shoulders, creating a stable plank posture.")
        elif elbow_angle < 80:
            human_opinion.append("Your elbows appear too tucked in, causing your shoulders to shift forward and reducing stability.")
        else:  # elbow_angle > 100
            human_opinion.append("Your elbows seem too extended forward, pushing your shoulders back and disrupting correct alignment.")

    return " ".join(human_opinion)