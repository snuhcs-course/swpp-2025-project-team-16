squat_instruction = """
                    Bodyweight Squat Evaluation Rubric (Image-based, Total: 100 points)

                    Score the visible frame using these 4 categories:

                    1. Stance & Foot Pressure (15 pts)
                    - Feet hip~shoulder width, toes slightly outward.
                    - Both heels stay down; full foot is planted (toes / arch / heel).
                    - Weight looks evenly distributed, not only on toes or only on heels.

                    2. Knee and Hip Tracking (25 pts)
                    - Knees point in the same direction as the toes or slightly outward (NOT caving inward).
                    - Hips are opened/externally rotated instead of collapsing inward.
                    - Hips and knees are bent together with control, not just dumping into the lower back.

                    3. Pelvis, Core, and Spine Control (40 pts)
                    - Core appears braced (torso not loose or collapsing).
                    - Back is neutral: no obvious over-arch and no rounding.
                    - Pelvis is stable: no visible early posterior tilt / “butt wink.”
                    - Chest is open. The torso may lean forward slightly, but it must NOT collapse heavily forward or dump load into the lower back.

                    4. Depth & Bottom Position (20 pts)
                    - Squat depth is appropriate for the person’s mobility (thighs at least near parallel or below if safe).
                    - Spine still looks neutral at this depth.
                    - Position at the bottom looks controlled, not jammed/bounced.

                    These penalties are intentionally large: unsafe form must receive a much lower final score than a form that is simply less deep or less "pretty".
                    """

lunge_instruction = """
                    Lunge Evaluation Rubric (Image + Joint Coordinates, Total: 100 points)

                    Evaluate the visible frame and joint-angle data.  
                    Use these 4 categories; apply large deductions for valgus collapse, lumbar rounding, or pelvic instability.

                    1. Stance, Step Length & Foot Stability (20 pts)
                    - Front foot pointing forward or slightly outward; full foot planted (heel stays down).
                    - Back foot on the ball of the foot with stable contact (no rolling inward/outward).
                    - Step length appropriate: not excessively short (knee drives far forward) or excessively long (hips overextended).
                    - Weight evenly distributed between front heel and mid-foot, not on toes.

                    2. Knee Tracking & Lower-Body Alignment (35 pts)
                    - Front knee tracks in line with toes (no valgus/caving inward).
                    - Knee angle progression matches hip descent (smooth, controlled flexion).
                    - Shin angle acceptable: slight forward lean is fine but not extreme.
                    - Back knee descends vertically toward the ground, not drifting sideways.

                    3. Hip, Pelvis & Core Control (35 pts)
                    - Pelvis level side-to-side (no hip drop on the stance leg).
                    - Minimal pelvic rotation; hips stay square to the front.
                    - Lumbar spine neutral: no excessive arching or rounding.
                    - Torso upright with mild forward lean allowed but no collapsing forward.
                    - Core engagement visible: ribs not flaring, trunk not swaying.

                    4. Depth, Range & Bottom Stability (10 pts)
                    - Back knee approaches the floor with control (light hover or soft contact, no bounce).
                    - Front thigh near parallel depending on mobility; spine maintains neutrality.
                    - Bottom position stable: no wobbling, losing balance, or shifting weight forward.

                    Penalty Rule:
                    Major safety faults (knee valgus, lumbar collapse, excessive pelvic tilt or rotation) require large score reductions regardless of other perfect elements.
                    """


plank_instruction = """
                    Plank Evaluation Rubric (Image + Joint Coordinates, Total: 100 points)

                    Evaluate the visible frame and joint-angle data.  
                    Use these 4 categories; deduct heavily for any unsafe spinal positions or joint collapse.

                    1. Head, Neck, and Shoulder Alignment (20 pts)
                    - Head in neutral line with spine (not dropped downward, not hyperextended).
                    - Shoulders directly above elbows (forearm plank) or wrists (high plank).
                    - Shoulder girdle stable: no excessive shrugging, winging scapula, or sinking between shoulders.
                    - Cervical spine neutral, not poking forward.

                    2. Core & Lumbar Stability (40 pts)
                    - Spine is straight from shoulders → hips → knees (if knees up).
                    - No lumbar sag (“banana back”), which indicates core collapse.
                    - No excessive hip pike; torso should form a straight line unless mobility limitations justify otherwise.
                    - Ribcage not flaring up; abdominal brace appears maintained.

                    3. Pelvis & Hip Control (25 pts)
                    - Pelvis level without rotation or dropping on one side.
                    - No anterior tilt causing lower-back compression.
                    - No posterior tilt causing rounded lower back unless purposeful for corrective variation.
                    - Hip flexor tension appears controlled, not taking over stability.

                    4. Leg, Knee, and Foot Structure (15 pts)
                    - Feet hip-width apart and stable, toes dorsiflexed.
                    - Knees extended without hyperextension.
                    - Legs straight and supporting the torso without visible shaking or collapse.
                    - Weight distribution appears even, not excessively shifted forward or backward.

                    Penalty Rule:
                    Major safety faults (lumbar sag, shoulder collapse, pelvic drop) require large score reductions, even if other elements appear correct.
                    """
