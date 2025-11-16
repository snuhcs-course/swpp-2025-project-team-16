package com.fitquest.app.ui.coachutils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.fitquest.app.R
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * OverlayView
 *
 * - PoseLandmarker가 준 landmark 좌표를 기반으로
 *   점/선(스켈레톤)과 관절 각도(예: 팔꿈치, 무릎)를 캔버스에 오버레이한다.
 */
class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

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
        linePaint.color = ContextCompat.getColor(context, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val poseLandmarkerResult = results ?: return

        // 1) 포인트(관절)와 스켈레톤 라인
        for (landmarkList in poseLandmarkerResult.landmarks()) {
            // draw keypoints
            for (lm in landmarkList) {
                canvas.drawPoint(
                    lm.x() * imageWidth * scaleFactor + offsetX,
                    lm.y() * imageHeight * scaleFactor + offsetY,
                    pointPaint
                )
            }

            // connect keypoints using MediaPipe's skeleton topology
            PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                canvas.drawLine(
                    landmarkList[connection!!.start()].x() * imageWidth * scaleFactor + offsetX,
                    landmarkList[connection.start()].y() * imageHeight * scaleFactor + offsetY,
                    landmarkList[connection.end()].x() * imageWidth * scaleFactor + offsetX,
                    landmarkList[connection.end()].y() * imageHeight * scaleFactor + offsetY,
                    linePaint
                )
            }
        }

        // 2) 각도 계산용 주요 관절들
        val landmarks = poseLandmarkerResult.landmarks().getOrNull(0) ?: return

        val lShoulder = landmarks[11]; val rShoulder = landmarks[12]
        val lElbow    = landmarks[13]; val rElbow    = landmarks[14]
        val lWrist    = landmarks[15]; val rWrist    = landmarks[16]

        val lHip      = landmarks[23]; val rHip      = landmarks[24]
        val lKnee     = landmarks[25]; val rKnee     = landmarks[26]
        val lAnkle    = landmarks[27]; val rAnkle    = landmarks[28]

        fun mid(
            a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
            b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
        ) = floatArrayOf((a.x() + b.x()) / 2f, (a.y() + b.y()) / 2f, (a.z() + b.z()) / 2f)

        val shoulderMid = mid(lShoulder, rShoulder)
        val hipMid = mid(lHip, rHip)
        val kneeMid = mid(lKnee, rKnee) // currently unused but kept for possible core angle calc

        // 3) 간단한 2D 각도(팔꿈치, 무릎, 몸통 굴곡 등)
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

        // 4) 텍스트 스타일
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        fun Canvas.drawAngleAtPivot(
            p: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
            text: String
        ) {
            drawText(
                text,
                p.x() * imageWidth * scaleFactor + offsetX,
                p.y() * imageHeight * scaleFactor + offsetY - 20f,
                textPaint
            )
        }

        // 팔꿈치/무릎 각도 라벨
        canvas.drawAngleAtPivot(lElbow,  "${leftElbowAngle.toInt()}°")
        canvas.drawAngleAtPivot(rElbow,  "${rightElbowAngle.toInt()}°")
        canvas.drawAngleAtPivot(lKnee,   "${leftKneeAngle.toInt()}°")
        canvas.drawAngleAtPivot(rKnee,   "${rightKneeAngle.toInt()}°")

        // 코어 굴곡(Flex) 각도 라벨
        canvas.drawText(
            "Flex: ${flexAngle.toInt()}°",
            hipMid[0] * imageWidth * scaleFactor + offsetX,
            hipMid[1] * imageHeight * scaleFactor + offsetY - 60f,
            textPaint
        )
    }

    /**
     * Fragment에서 pose 결과 받을 때마다 호출해주는 함수.
     * 이미지 사이즈/스케일 정보를 저장하고 invalidate()로 다시 그리게 한다.
     */
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
                // 프리뷰는 화면을 채우기 때문에 max로 맞추고 중앙 offset
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }

        offsetX = (width - imageWidth * scaleFactor) / 2f
        offsetY = (height - imageHeight * scaleFactor) / 2f

        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }

    private fun calculateAngle2D(
        ax: Float, ay: Float,
        bx: Float, by: Float,   // pivot (각도의 꼭짓점)
        cx: Float, cy: Float
    ): Double {
        val v1x = ax - bx; val v1y = ay - by
        val v2x = cx - bx; val v2y = cy - by
        val dot = (v1x * v2x + v1y * v2y).toDouble()

        val n1 = sqrt((v1x * v1x + v1y * v1y).toDouble()).coerceAtLeast(1e-8)
        val n2 = sqrt((v2x * v2x + v2y * v2y).toDouble()).coerceAtLeast(1e-8)

        val cosT = (dot / (n1 * n2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosT))
    }
}
