package com.aisportspt.app.ui.fragments.coachutils

/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

import com.aisportspt.app.R
import kotlin.math.acos

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var offsetX = 0f
    private var offsetY = 0f

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor + offsetX,
                        normalizedLandmark.y() * imageHeight * scaleFactor + offsetY,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start())
                            .x() * imageWidth * scaleFactor + offsetX,
                        poseLandmarkerResult.landmarks().get(0).get(it.start())
                            .y() * imageHeight * scaleFactor + offsetY,
                        poseLandmarkerResult.landmarks().get(0).get(it.end())
                            .x() * imageWidth * scaleFactor + offsetX,
                        poseLandmarkerResult.landmarks().get(0).get(it.end())
                            .y() * imageHeight * scaleFactor + offsetY,
                        linePaint
                    )
                }
            }

            val landmarks = poseLandmarkerResult.landmarks().getOrNull(0) ?: return

// -------- 3D 각도 계산을 위한 좌표 꺼내기 --------
            val lShoulder = landmarks[11]; val rShoulder = landmarks[12]
            val lElbow    = landmarks[13]; val rElbow    = landmarks[14]
            val lWrist    = landmarks[15]; val rWrist    = landmarks[16]

            val lHip      = landmarks[23]; val rHip      = landmarks[24]
            val lKnee     = landmarks[25]; val rKnee     = landmarks[26]
            val lAnkle    = landmarks[27]; val rAnkle    = landmarks[28]

            fun mid(a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
                    b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark)
                    = floatArrayOf((a.x() + b.x()) / 2f, (a.y() + b.y()) / 2f, (a.z() + b.z()) / 2f)

            val shoulderMid = mid(lShoulder, rShoulder)
            val hipMid = mid(lHip, rHip)
            val kneeMid = mid(lKnee, rKnee)

//// -------- 팔꿈치 각(좌/우) [Shoulder-Elbow-Wrist] --------
//            val leftElbowAngle = calculateAngle3D(
//                lShoulder.x(), lShoulder.y(), lShoulder.z(),
//                lElbow.x(),    lElbow.y(),    lElbow.z(),     // pivot
//                lWrist.x(),    lWrist.y(),    lWrist.z()
//            )
//
//            val rightElbowAngle = calculateAngle3D(
//                rShoulder.x(), rShoulder.y(), rShoulder.z(),
//                rElbow.x(),    rElbow.y(),    rElbow.z(),     // pivot
//                rWrist.x(),    rWrist.y(),    rWrist.z()
//            )
//
//// -------- 무릎 각(좌/우) [Hip-Knee-Ankle] --------
//            val leftKneeAngle = calculateAngle3D(
//                lHip.x(),   lHip.y(),   lHip.z(),
//                lKnee.x(),  lKnee.y(),  lKnee.z(),          // pivot
//                lAnkle.x(), lAnkle.y(), lAnkle.z()
//            )
//
//            val rightKneeAngle = calculateAngle3D(
//                rHip.x(),   rHip.y(),   rHip.z(),
//                rKnee.x(),  rKnee.y(),  rKnee.z(),          // pivot
//                rAnkle.x(), rAnkle.y(), rAnkle.z()
//            )
//
//            val flexAngle = calculateAngle3D(
//                shoulderMid[0], shoulderMid[1], shoulderMid[2],
//                hipMid[0], hipMid[1], hipMid[2],   // pivot
//                kneeMid[0], kneeMid[1], kneeMid[2]
//            )

            // ---------- 2D 각도 계산 ----------
            val leftElbowAngle = calculateAngle2D(
                lShoulder.x(), lShoulder.y(),
                lElbow.x(), lElbow.y(),
                lWrist.x(), lWrist.y()
            )

            val rightElbowAngle = calculateAngle2D(
                rShoulder.x(), rShoulder.y(),
                rElbow.x(), rElbow.y(),
                rWrist.x(), rWrist.y()
            )

            val leftKneeAngle = calculateAngle2D(
                lHip.x(), lHip.y(),
                lKnee.x(), lKnee.y(),
                lAnkle.x(), lAnkle.y()
            )

            val rightKneeAngle = calculateAngle2D(
                rHip.x(), rHip.y(),
                rKnee.x(), rKnee.y(),
                rAnkle.x(), rAnkle.y()
            )

            val flexAngle = calculateAngle2D(
                shoulderMid[0], shoulderMid[1],
                hipMid[0], hipMid[1],
                kneeMid[0], kneeMid[1]
            )

// -------- 텍스트 스타일 --------
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // -------- 화면에 각도 표시 (pivot 위치 근처) --------
            fun Canvas.drawAngleAtPivot(p: com.google.mediapipe.tasks.components.containers.NormalizedLandmark, text: String) {
                drawText(
                    text,
                    p.x() * imageWidth * scaleFactor + offsetX,
                    p.y() * imageHeight * scaleFactor + offsetY - 20f,
                    textPaint
                )
            }

// 팔꿈치 각도 표시
            canvas.drawAngleAtPivot(lElbow,  "${leftElbowAngle.toInt()}°")
            canvas.drawAngleAtPivot(rElbow,  "${rightElbowAngle.toInt()}°")

// 무릎 각도 표시
            canvas.drawAngleAtPivot(lKnee,   "${leftKneeAngle.toInt()}°")
            canvas.drawAngleAtPivot(rKnee,   "${rightKneeAngle.toInt()}°")

            canvas.drawText(
                "Flex: ${flexAngle.toInt()}°",
                hipMid[0] * imageWidth * scaleFactor + offsetX,
                hipMid[1] * imageHeight * scaleFactor + offsetY - 60f,
                textPaint
            )
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        offsetX = (width  - imageWidth  * scaleFactor) / 2f
        offsetY = (height - imageHeight * scaleFactor) / 2f
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }

    private fun calculateAngle3D(
        ax: Float, ay: Float, az: Float,   // point A
        bx: Float, by: Float, bz: Float,   // pivot B (각도의 꼭짓점)
        cx: Float, cy: Float, cz: Float    // point C
    ): Double {
        val v1x = ax - bx; val v1y = ay - by; val v1z = az - bz
        val v2x = cx - bx; val v2y = cy - by; val v2z = cz - bz

        val dot  = (v1x*v2x + v1y*v2y + v1z*v2z).toDouble()
        val n1   = kotlin.math.sqrt((v1x*v1x + v1y*v1y + v1z*v1z).toDouble()).coerceAtLeast(1e-8)
        val n2   = kotlin.math.sqrt((v2x*v2x + v2y*v2y + v2z*v2z).toDouble()).coerceAtLeast(1e-8)
        val cosT = (dot / (n1*n2)).coerceIn(-1.0, 1.0)

        return Math.toDegrees(kotlin.math.acos(cosT))
    }

    private fun calculateAngle2D(
        ax: Float, ay: Float,
        bx: Float, by: Float,   // pivot
        cx: Float, cy: Float
    ): Double {
        val v1x = ax - bx; val v1y = ay - by
        val v2x = cx - bx; val v2y = cy - by
        val dot = (v1x * v2x + v1y * v2y).toDouble()
        val n1 = kotlin.math.sqrt((v1x*v1x + v1y*v1y).toDouble()).coerceAtLeast(1e-8)
        val n2 = kotlin.math.sqrt((v2x*v2x + v2y*v2y).toDouble()).coerceAtLeast(1e-8)
        val cosT = (dot / (n1 * n2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosT))
    }

}