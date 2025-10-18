package com.aisportspt.app.ui.fragments.coachutils.counter

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min

class SitUpCounter : BaseCounter() {

    enum class Phase { DOWN, UP_GOING, UP }

    override var count = 0
    override var phase: String = "DOWN"

    private var phaseState: Phase = Phase.UP
        set(value) {
            field = value
            phase = value.name            // enum 변경되면 문자열도 동기화
        }

    private var flex = 0.0
    private var torsoY = 0.0
    private var emaInit = false
    private val alpha = 0.9

    private var baselineTorsoY = Double.NaN
    private var calibUntilMs = Long.MIN_VALUE
    private var lastRepMs = 0L

    private val FLEX_UP = 80.0
    private val FLEX_DOWN = 120.0
    private val TORSO_RISE = 0.07
    private val MIN_REP_INTERVAL = 400

    override fun reset(nowMs: Long) {
        count = 0
        phaseState = Phase.DOWN
        emaInit = false
        baselineTorsoY = Double.NaN
        lastRepMs = 0L
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

        val shL=v(11); val shR=v(12); val hipL=v(23); val hipR=v(24); val knL=v(25); val knR=v(26)
        val hip = mid(hipL, hipR)
        val sh  = mid(shL, shR)
        val kneeMid = mid(knL, knR)

        val flexNow = angle(sub(sh, hip), sub(kneeMid, hip))
        val torso = (sh[1] + hip[1]) / 2.0

        if (!emaInit) { flex = flexNow; torsoY = torso; emaInit = true }
        else {
            fun ema(p: Double, x: Double) = alpha*x + (1-alpha)*p
            flex = ema(flex, flexNow); torsoY = ema(torsoY, torso)
        }

        if (nowMs <= calibUntilMs) {
            baselineTorsoY = if (baselineTorsoY.isNaN()) torsoY else 0.9*baselineTorsoY + 0.1*torsoY
            phaseState = Phase.DOWN
            return
        }
        if (baselineTorsoY.isNaN()) baselineTorsoY = torsoY

        val rise = torsoY - baselineTorsoY

        when (phaseState) {
            Phase.DOWN -> {
                if (flex < FLEX_DOWN && rise > TORSO_RISE*0.5) phaseState = Phase.UP_GOING
            }
            Phase.UP_GOING -> {
                if (flex < FLEX_UP && rise > TORSO_RISE) {
                    if (nowMs - lastRepMs >= MIN_REP_INTERVAL) { count += 1; lastRepMs = nowMs }
                    phaseState = Phase.UP
                } else if (flex > FLEX_DOWN) {
                    phaseState = Phase.DOWN
                }
            }
            Phase.UP -> if (flex > FLEX_DOWN) phaseState = Phase.DOWN
        }
    }
}
