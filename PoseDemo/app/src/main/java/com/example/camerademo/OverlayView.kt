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
package com.example.camerademo

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

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

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
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start())
                            .x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.start())
                            .y() * imageHeight * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end())
                            .x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end())
                            .y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }

            val landmarks = poseLandmarkerResult.landmarks().getOrNull(0) ?: return

            
            val leftHip = landmarks[24]
            val leftKnee = landmarks[26]
            val leftAnkle = landmarks[28]
            val leftAngle = calculateAngle(
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
                leftAnkle.x(), leftAnkle.y()
            )

            // 오른쪽 무릎 (23-25-27)
            val rightHip = landmarks[23]
            val rightKnee = landmarks[25]
            val rightAnkle = landmarks[27]
            val rightAngle = calculateAngle(
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
                rightAnkle.x(), rightAnkle.y()
            )

            // 텍스트 스타일
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // 왼쪽 무릎 각도 표시
            canvas.drawText(
                "${leftAngle.toInt()}°",
                leftKnee.x() * imageWidth * scaleFactor,
                leftKnee.y() * imageHeight * scaleFactor - 20,
                textPaint
            )

            // 오른쪽 무릎 각도 표시
            canvas.drawText(
                "${rightAngle.toInt()}°",
                rightKnee.x() * imageWidth * scaleFactor,
                rightKnee.y() * imageHeight * scaleFactor - 20,
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
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }

    private fun calculateAngle(
        x1: Float, y1: Float, // hip
        x2: Float, y2: Float, // knee (pivot)
        x3: Float, y3: Float  // ankle
    ): Double {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2

        val dot = v1x * v2x + v1y * v2y
        val norm1 = Math.sqrt((v1x*v1x + v1y*v1y).toDouble())
        val norm2 = Math.sqrt((v2x*v2x + v2y*v2y).toDouble())

        return Math.toDegrees(Math.acos(dot / (norm1 * norm2)))
    }
}