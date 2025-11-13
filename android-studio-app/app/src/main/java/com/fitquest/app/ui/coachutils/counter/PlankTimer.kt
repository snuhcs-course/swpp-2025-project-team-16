package com.fitquest.app.ui.coachutils.counter

import kotlin.math.*

/**
 * PlankTimer
 * - 폼이 좋을 때만 누적 시간을 증가(hold). 폼이 흐트러지면 "초기화"하지 않고 "일시정지".
 * - count(Int): floor(누적초)
 * - phase: NOT_READY / HOLDING / REST
 * - holdMs(): 현재까지의 누적 ms
 * - holdSeconds(): Double(소수 1자리로 표시 권장)
 */
class PlankTimer : BaseCounter() {

    enum class Phase { NOT_READY, HOLDING, REST }

    override var count: Int = 0
    override var phase: String = "NOT_READY"

    private var phaseState: Phase = Phase.NOT_READY
        set(value) {
            field = value
            phase = value.name
        }

    /** 누적 '좋은 폼' 보유 시간(ms). 폼이 깨져도 이 값은 초기화하지 않음 */
    private var totalHoldMs = 0L

    /** 내부 시간 측정용 */
    private var lastUpdateMs = 0L

    // ---- 폼 판정 임계값 ----
    private val MAX_BODY_DEVIATION = 18.0   // 어깨–엉덩이–발목 180° 기준 허용 오차
    private val MIN_ELBOW_ANGLE = 70.0      // 팔꿈치 최소 각도

    override fun reset(nowMs: Long) {
        phaseState = Phase.NOT_READY
        count = 0
        totalHoldMs = 0L
        lastUpdateMs = nowMs
    }

    /** UI에서 소수 1자리 표시용 */
    fun holdMs(): Long = totalHoldMs
    fun holdSeconds(): Double = totalHoldMs / 1000.0

    override fun update(points: FloatArray, nowMs: Long) {
        if (points.size < 33 * 3) return
        if (lastUpdateMs == 0L) lastUpdateMs = nowMs
        val dt = (nowMs - lastUpdateMs).coerceAtLeast(0L)
        lastUpdateMs = nowMs

        // ---- 유틸 ----
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
            val v1 = doubleArrayOf(a[0] - b[0], a[1] - b[1])
            val v2 = doubleArrayOf(c[0] - b[0], c[1] - b[1])
            val d = v1[0] * v2[0] + v1[1] * v2[1]
            val n1 = sqrt(v1[0] * v1[0] + v1[1] * v1[1])
            val n2 = sqrt(v2[0] * v2[0] + v2[1] * v2[1])
            val cosT = (d / (n1 * n2)).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosT))
        }
        fun mid(a: DoubleArray, b: DoubleArray) =
            doubleArrayOf((a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0)

        // ---- 관절 ----
        val shL = v(11); val shR = v(12)
        val hipL = v(23); val hipR = v(24)
        val ankL = v(27); val ankR = v(28)
        val elL = v(13); val elR = v(14)
        val wrL = v(15); val wrR = v(16)

        val shoulder = mid(shL, shR)
        val hip = mid(hipL, hipR)
        val ankle = mid(ankL, ankR)

        // ---- 폼 판정 ----
        val bodyAngle = angle3D(shoulder, hip, ankle)      // 180° 근처면 OK
        val bodyOk = kotlin.math.abs(180.0 - bodyAngle) < MAX_BODY_DEVIATION

        val elbowLA = angle2D(shL, elL, wrL)
        val elbowRA = angle2D(shR, elR, wrR)
        val elbowOk = min(elbowLA, elbowRA) > MIN_ELBOW_ANGLE

        val shoulderYDiffOk = kotlin.math.abs(shL[1] - shR[1]) < 0.2
        val hipYDiffOk = kotlin.math.abs(hipL[1] - hipR[1]) < 0.2
        val facingOk = shoulderYDiffOk && hipYDiffOk

        val formOk = bodyOk && elbowOk && facingOk

        // ---- 상태머신 & 누적 ----
        when (phaseState) {
            Phase.NOT_READY -> {
                if (formOk) {
                    phaseState = Phase.HOLDING
                    // 첫 진입 프레임부터 dt 가산
                    totalHoldMs += dt
                } else {
                    // 대기
                }
            }
            Phase.HOLDING -> {
                if (formOk) {
                    totalHoldMs += dt
                } else {
                    // "초기화" 금지: 누적 유지, 상태만 REST로 전환(일시정지)
                    phaseState = Phase.REST
                }
            }
            Phase.REST -> {
                if (formOk) {
                    phaseState = Phase.HOLDING
                    // 이어서 누적 계속
                    totalHoldMs += dt
                } else {
                    // 그대로 정지
                }
            }
        }

        // BaseCounter 요구 사양: 정수 카운트(초)
        count = (totalHoldMs / 1000L).toInt()
    }
}
