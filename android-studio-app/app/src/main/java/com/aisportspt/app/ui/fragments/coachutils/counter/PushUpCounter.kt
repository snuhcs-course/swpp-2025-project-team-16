package com.aisportspt.app.ui.fragments.coachutils.counter

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min

class PushUpCounter : BaseCounter(){

    enum class Phase { UP, DOWN, BOTTOM, UP_GOING }

    override var count = 0
    override var phase: String = "UP"

    private var phaseState: Phase = Phase.UP
        set(value) {
            field = value
            phase = value.name            // enum 변경되면 문자열도 동기화
        }

    // EMA
    private var elbowL = 180.0; private var elbowR = 180.0
    private var torsoY = 0.0
    private var emaInit = false
    private val alpha = 0.95

    // 기준
    private var baselineTorsoY = Double.NaN
    private var calibUntilMs = Long.MIN_VALUE

    // 타이머
    private var lastRepMs = 0L
    private var bottomEnterMs = 0L

    // 임계치
    private val ELBOW_UP = 140.0
    private val ELBOW_DOWN_ENTER = 120.0
    private val ELBOW_BOTTOM = 80.0
    private val TORSO_DROP = 0.04
    private val MIN_BOTTOM_HOLD = 100
    private val MIN_REP_INTERVAL = 300

    override fun reset(nowMs: Long) {
        count = 0
        phaseState = Phase.UP
        emaInit = false
        baselineTorsoY = Double.NaN
        lastRepMs = 0L
        bottomEnterMs = 0L
        calibUntilMs = nowMs + 800
    }

    override fun update(points: FloatArray, nowMs: Long) {
        if (points.size < 33*3) return

        fun idx(i: Int) = 3*i
        fun v(i: Int) = doubleArrayOf(points[idx(i)].toDouble(), points[idx(i)+1].toDouble(), points[idx(i)+2].toDouble())
        fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0]-b[0], a[1]-b[1], a[2]-b[2])
        fun dot(a: DoubleArray, b: DoubleArray) = a[0]*b[0]+a[1]*b[1]+a[2]*b[2]
        fun nrm(a: DoubleArray): Double { val t = kotlin.math.sqrt(dot(a,a)); return if (t<1e-8) 1e-8 else t }
        fun angle(a: DoubleArray, b: DoubleArray): Double { val c = dot(a,b)/(nrm(a)*nrm(b)); val cl = max(-1.0, min(1.0, c)); return Math.toDegrees(acos(cl)) }
        fun mid(a: DoubleArray, b: DoubleArray) = doubleArrayOf((a[0]+b[0])/2.0,(a[1]+b[1])/2.0,(a[2]+b[2])/2.0)
        fun angle2D(a: DoubleArray, b: DoubleArray): Double {
            val dot = a[0]*b[0] + a[1]*b[1]
            val n1 = kotlin.math.sqrt(a[0]*a[0] + a[1]*a[1])
            val n2 = kotlin.math.sqrt(b[0]*b[0] + b[1]*b[1])
            val cosT = (dot / (n1*n2)).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosT))
        }


        val shL=v(11); val shR=v(12); val elL=v(13); val elR=v(14); val wrL=v(15); val wrR=v(16)
        val hipL=v(23); val hipR=v(24)

//        val elbowLA = angle(sub(shL, elL), sub(wrL, elL))
        val elbowLA = angle2D(
            doubleArrayOf(shL[0]-elL[0], shL[1]-elL[1]),
            doubleArrayOf(wrL[0]-elL[0], wrL[1]-elL[1])
        )
        val elbowRA = angle2D(
            doubleArrayOf(shR[0]-elR[0], shR[1]-elR[1]),
            doubleArrayOf(wrR[0]-elR[0], wrR[1]-elR[1])
        )

//        val elbowRA = angle(sub(shR, elR), sub(wrR, elR))
        val midSh = mid(shL, shR)
        val midHip = mid(hipL, hipR)
        val torso = (midSh[1] + midHip[1]) / 2.0

        if (!emaInit) { elbowL=elbowLA; elbowR=elbowRA; torsoY=torso; emaInit=true }
        else {
            fun ema(p: Double, x: Double) = alpha*x + (1-alpha)*p
            elbowL=ema(elbowL, elbowLA); elbowR=ema(elbowR, elbowRA); torsoY=ema(torsoY, torso)
        }

        if (nowMs <= calibUntilMs) {
            baselineTorsoY = if (baselineTorsoY.isNaN()) torsoY else 0.9*baselineTorsoY + 0.1*torsoY
            phaseState = Phase.UP
            return
        }
        if (baselineTorsoY.isNaN()) baselineTorsoY = torsoY

        val elbowMin = kotlin.math.min(elbowL, elbowR)
        val drop = baselineTorsoY - torsoY

        when (phaseState) {
            Phase.UP -> if (elbowMin < ELBOW_DOWN_ENTER) phaseState = Phase.DOWN
            Phase.DOWN -> {
                if (elbowMin < ELBOW_BOTTOM) // && drop > TORSO_DROP)
                { phaseState = Phase.BOTTOM; bottomEnterMs = nowMs }
                else if (elbowMin > ELBOW_UP) phaseState = Phase.UP
            }
            Phase.BOTTOM -> if (nowMs - bottomEnterMs >= MIN_BOTTOM_HOLD) phaseState = Phase.UP_GOING
            Phase.UP_GOING -> {
                val upOk = (elbowMin > ELBOW_UP) // && (drop < TORSO_DROP*0.5)
                if (upOk) {
                    if (nowMs - lastRepMs >= MIN_REP_INTERVAL) { count += 1; lastRepMs = nowMs }
                    phaseState = Phase.UP
                } else if (elbowMin < ELBOW_BOTTOM) // && drop > TORSO_DROP)
                {
                    phaseState = Phase.BOTTOM
                    bottomEnterMs = nowMs
                }
            }
        }
    }
}
