package com.fitquest.app.ui.coachutils.counter

import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.sqrt


class LungeCounter : BaseCounter() {

    enum class Phase { UP, DOWN_REACHED }

    override var count: Int = 0
    override var phase: String = "UP"

    private var phaseState = Phase.UP
        set(value) {
            field = value
            phase = value.name
        }

    private var lastUpdateMs = 0L

    override fun reset(nowMs: Long) {
        count = 0
        phaseState = Phase.UP
        lastUpdateMs = nowMs
    }


    override fun update(points: FloatArray, nowMs: Long) {
        if (points.size < 33 * 3) return

        if (lastUpdateMs == 0L) lastUpdateMs = nowMs
        lastUpdateMs = nowMs

        fun v(i: Int) = doubleArrayOf(
            points[i * 3].toDouble(),
            points[i * 3 + 1].toDouble(),
            points[i * 3 + 2].toDouble()
        )

        fun sub(a: DoubleArray, b: DoubleArray) =
            doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])

        fun dot(a: DoubleArray, b: DoubleArray) =
            a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

        fun nrm(a: DoubleArray): Double {
            val t = sqrt(dot(a, a))
            return if (t < 1e-8) 1e-8 else t
        }

        fun angle(a: DoubleArray, b: DoubleArray, c: DoubleArray): Double {
            val v1 = sub(a, b)
            val v2 = sub(c, b)
            val cosT = (dot(v1, v2) / (nrm(v1) * nrm(v2))).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosT))
        }


        /** 1. 앞/뒷다리 자동판정 */
        val ankleL = v(27)
        val ankleR = v(28)

        val isLeftBackLeg =
            (ankleL[2] > ankleR[2]) ||
                    (ankleL[1] < ankleR[1])

        val hipBack = if (isLeftBackLeg) v(23) else v(24)
        val kneeBack = if (isLeftBackLeg) v(25) else v(26)
        val ankleBack = if (isLeftBackLeg) v(27) else v(28)

        /** 2. 무릎 각도 */
        val kneeAngle = angle(hipBack, kneeBack, ankleBack)


        /** 3. FSM */
        when (phaseState) {

            Phase.UP -> {
                if (kneeAngle < 120.0) {
                    phaseState = Phase.DOWN_REACHED
                }
            }

            Phase.DOWN_REACHED -> {
                if (kneeAngle > 150.0) {
                    count++
                    phaseState = Phase.UP
                }
            }
        }
    }
}
