package com.aisportspt.app.ui.fragments.coachutils.counter

import kotlin.math.*

class PlankTimer : BaseCounter() {

    enum class Phase { NOT_READY, HOLDING, REST }

    override var count = 0
    override var phase = "NOT_READY"

    private var phaseState: Phase = Phase.NOT_READY
        set(value) {
            field = value
            phase = value.name
        }

    var elapsedMs = 0L       // 현재 홀드 구간 경과
        private set
    var bestStreakMs = 0L    // 최장 홀드
        private set
    var totalGoodMs = 0L     // 폼 OK 누적

    // EMA 필터
    private var pelvisY = 0.0
    private var shoulderY = 0.0
    private var ankleY = 0.0
    private var emaInit = false
    private val alpha = 0.95

    private var lastUpdateMs = 0L

    // --- 옆모습용 기준 ---
    private val MAX_BODY_DEVIATION = 18.0   // 어깨–엉덩이–발목 180° 기준 오차 허용
    private val MAX_TILT_DEG = 20.0         // 몸 전체 기울기 허용
    private val MAX_HIP_OFFSET = 0.06       // 어깨–발목 선에서 골반 편차
    private val MIN_ELBOW_ANGLE = 70.0      // 팔꿈치 최소 각도

    override fun reset(nowMs: Long) {
        phaseState = Phase.NOT_READY
        elapsedMs = 0L
        bestStreakMs = 0L
        totalGoodMs = 0L
        emaInit = false
        lastUpdateMs = nowMs
    }

    override fun update(points: FloatArray, nowMs: Long) {
        if (points.size < 33 * 3) return

        // --- vector util ---
        fun idx(i: Int) = 3 * i
        fun v(i: Int) = doubleArrayOf(
            points[idx(i)].toDouble(),
            points[idx(i) + 1].toDouble(),
            points[idx(i) + 2].toDouble()
        )
        fun sub(a: DoubleArray, b: DoubleArray) =
            doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
        fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
        fun nrm(a: DoubleArray): Double {
            val t = sqrt(dot(a, a))
            return if (t < 1e-8) 1e-8 else t
        }
        fun angle3D(a: DoubleArray, b: DoubleArray, c: DoubleArray): Double {
            // a-b-c (pivot b)
            val v1 = sub(a, b)
            val v2 = sub(c, b)
            val cosT = (dot(v1, v2) / (nrm(v1) * nrm(v2))).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosT))
        }

        fun angle2D(a: DoubleArray, b: DoubleArray, c: DoubleArray): Double {
            // pivot b 기준: a-b-c
            val v1 = doubleArrayOf(a[0] - b[0], a[1] - b[1])
            val v2 = doubleArrayOf(c[0] - b[0], c[1] - b[1])
            val dot = v1[0]*v2[0] + v1[1]*v2[1]
            val n1 = sqrt(v1[0]*v1[0] + v1[1]*v1[1])
            val n2 = sqrt(v2[0]*v2[0] + v2[1]*v2[1])
            val cosT = (dot / (n1 * n2)).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosT))
        }
        fun mid(a: DoubleArray, b: DoubleArray) =
            doubleArrayOf((a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0)

        // --- 좌표 ---
        val shL = v(11); val shR = v(12)
        val hipL = v(23); val hipR = v(24)
        val ankL = v(27); val ankR = v(28)
        val elL = v(13); val elR = v(14)
        val wrL = v(15); val wrR = v(16)

        val shoulder = mid(shL, shR)
        val hip = mid(hipL, hipR)
        val ankle = mid(ankL, ankR)

        // --- y–z 평면 투영 ---
        fun yz(p: DoubleArray) = doubleArrayOf(p[1], p[2]) // (y,z)
        fun sub2(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1])
        fun dot2(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1]
        fun nrm2(a: DoubleArray) = sqrt(a[0] * a[0] + a[1] * a[1])

        // --- ① 몸통 직선성 (어깨–엉덩이–발목 각도) ---
        val bodyAngle = angle3D(shoulder, hip, ankle) // 실제 3D 각도 이용
        val bodyOk = abs(170.0 - bodyAngle) < MAX_BODY_DEVIATION

        // --- ② 기울기 (몸 전체 수평도) ---
        val shoulderAnkle2D = sub2(yz(ankle), yz(shoulder))
        val tiltDeg = Math.toDegrees(atan2(shoulderAnkle2D[0], abs(shoulderAnkle2D[1])))
        val tiltOk = abs(tiltDeg) < MAX_TILT_DEG

//        // --- ③ 골반 오프셋 (hip offset 거리) ---
//        val lineVec = sub(ankle, shoulder)
//        val unit = doubleArrayOf(lineVec[0] / nrm(lineVec), lineVec[1] / nrm(lineVec), lineVec[2] / nrm(lineVec))
//        val sp = sub(hip, shoulder)
//        val projLen = dot(sp, unit)
//        val proj = doubleArrayOf(unit[0] * projLen, unit[1] * projLen, unit[2] * projLen)
//        val offsetVec = sub(sp, proj)
//        val hipOffset = abs(offsetVec[1])  // y축 기준 오프셋
//        val hipOk = hipOffset < MAX_HIP_OFFSET

        // --- ④ 팔꿈치 각도 ---
//        val elbowLA = angle3D(shL, elL, wrL)
//        val elbowRA = angle3D(shR, elR, wrR)
//        val elbowMin = min(elbowLA, elbowRA)
//        val elbowOk = elbowMin > MIN_ELBOW_ANGLE
        val elbowLA = angle2D(shL, elL, wrL)
        val elbowRA = angle2D(shR, elR, wrR)
        val elbowMin = min(elbowLA, elbowRA)
        val elbowOk = elbowMin > MIN_ELBOW_ANGLE

        // 좌우 어깨 높이 차이
        val shoulderYDiff = abs(shL[1] - shR[1]) < 0.2
// 좌우 엉덩이 높이 차이
        val hipYDiff = abs(hipL[1] - hipR[1]) < 0.2

        val facingDown = shoulderYDiff && hipYDiff


        // --- 최종 폼 판정 ---
        val formOk = bodyOk && elbowOk && facingDown// && tiltOk

        // --- 상태 머신 ---
        val dt = (nowMs - lastUpdateMs).coerceAtLeast(0L)
        lastUpdateMs = nowMs

        when (phaseState) {
            Phase.NOT_READY -> {
                if (formOk) {
                    phaseState = Phase.HOLDING
                    elapsedMs = 0L
                }
            }
            Phase.HOLDING -> {
                if (formOk) {
                    elapsedMs += dt
                    totalGoodMs += dt
                    if (elapsedMs > bestStreakMs) bestStreakMs = elapsedMs
                } else {
                    phaseState = Phase.REST
                }
            }
            Phase.REST -> {
                if (formOk) {
                    phaseState = Phase.HOLDING
                    elapsedMs = 0L
                }
            }
        }
    }
}
