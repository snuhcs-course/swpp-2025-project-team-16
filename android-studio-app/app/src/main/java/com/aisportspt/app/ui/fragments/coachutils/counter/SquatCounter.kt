package com.aisportspt.app.ui.fragments.coachutils.counter

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SquatCounter : BaseCounter(){
    enum class Phase { UP, DOWN, BOTTOM, UP_GOING }

    override var count = 0
    override var phase: String = "UP"

    private var phaseState: Phase = Phase.UP
        set(value) {
            field = value
            phase = value.name            // enum 변경되면 문자열도 동기화
        }

    // EMA 상태
    private var kneeL = 180.0; private var kneeR = 180.0
    private var hipL  = 180.0; private var hipR  = 180.0
    private var pelvisY = 0.0
    private var emaInit = false
    private val alpha = 0.2

    // 기준(서있는 자세에서 자동 보정)
    private var baselinePelvisY = Double.NaN
    private var calibUntilMs = Long.MIN_VALUE

    // 타이머
    private var lastRepMs = 0L
    private var bottomEnterMs = 0L

    // 임계치
    private val KNEE_UP = 160.0
    private val KNEE_DOWN_ENTER = 140.0
    private val KNEE_BOTTOM = 100.0
    private val KNEE_UP_GOING = 130.0

    private val PELVIS_DROP = 0.12      // 정규화 y에서 대략 12% 이상 하강
    private val MIN_REP_INTERVAL = 300  // ms
    private val MIN_BOTTOM_HOLD = 100   // ms

    override fun reset(nowMs: Long) {
        count = 0
        phaseState = Phase.UP
        emaInit = false
        baselinePelvisY = Double.NaN
        lastRepMs = 0
        bottomEnterMs = 0
        calibUntilMs = nowMs + 200 // 0.8s 동안 서 있는 기준 캡처
    }

    /**
     * points: FloatArray [x,y,z,...] (MediaPipe landmark 순서)
     * nowMs : 프레임 타임스탬프(ms)
     */
    override fun update(points: FloatArray, nowMs: Long) {
        if (points.size < 33 * 3) return

        fun idx(i: Int) = 3 * i
        fun v(i: Int) = doubleArrayOf(points[idx(i)].toDouble(), points[idx(i)+1].toDouble(), points[idx(i)+2].toDouble())
        fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0]-b[0], a[1]-b[1], a[2]-b[2])
        fun dot(a: DoubleArray, b: DoubleArray) = a[0]*b[0] + a[1]*b[1] + a[2]*b[2]
        fun nrm(a: DoubleArray): Double {
            val t = sqrt(dot(a,a))
            return if (t<1e-8) 1e-8 else t
        }
        fun angle(a: DoubleArray, b: DoubleArray): Double {
            val c = dot(a,b) / (nrm(a)*nrm(b))
            val clamped = max(-1.0, min(1.0, c))
            return Math.toDegrees(acos(clamped))
        }

        val hipLpt = v(23); val hipRpt = v(24)
        val kneeLpt = v(25); val kneeRpt = v(26)
        val ankLpt = v(27); val ankRpt = v(28)
        val shLpt = v(11);  val shRpt = v(12)

        val pelvis = doubleArrayOf((hipLpt[0]+hipRpt[0])/2.0, (hipLpt[1]+hipRpt[1])/2.0, (hipLpt[2]+hipRpt[2])/2.0)
        val midShoulder = doubleArrayOf((shLpt[0]+shRpt[0])/2.0, (shLpt[1]+shRpt[1])/2.0, (shLpt[2]+shRpt[2])/2.0)

        // 무릎각
        val kneeLA = angle(sub(hipLpt, kneeLpt), sub(ankLpt, kneeLpt))
        val kneeRA = angle(sub(hipRpt, kneeRpt), sub(ankRpt, kneeRpt))
        // 엉덩각(힙 굴곡)
        val hipLA  = angle(sub(midShoulder, hipLpt), sub(kneeLpt, hipLpt))
        val hipRA  = angle(sub(midShoulder, hipRpt), sub(kneeRpt, hipRpt))

        // EMA
        if (!emaInit) {
            kneeL = kneeLA; kneeR = kneeRA; hipL = hipLA; hipR = hipRA; pelvisY = pelvis[1]
            emaInit = true
        } else {
            fun ema(prev: Double, x: Double) = alpha * x + (1 - alpha) * prev
            kneeL = ema(kneeL, kneeLA); kneeR = ema(kneeR, kneeRA)
            hipL  = ema(hipL, hipLA);   hipR  = ema(hipR, hipRA)
            pelvisY = ema(pelvisY, pelvis[1])
        }

        // 초기 서있는 기준 보정
        if (nowMs <= calibUntilMs) {
            baselinePelvisY = if (baselinePelvisY.isNaN()) pelvisY else 0.9*baselinePelvisY + 0.1*pelvisY
            phaseState = Phase.UP
            return
        }
        if (baselinePelvisY.isNaN()) baselinePelvisY = pelvisY

        val kneeMin = min(kneeL, kneeR)
        val hipMin  = min(hipL, hipR)
        val drop = (baselinePelvisY - pelvisY) // 내려갈수록 양수 (y 위쪽 기준이면 1 - y를 쓰세요)

        when (phaseState) {
            Phase.UP -> {
                if (kneeMin < KNEE_DOWN_ENTER) {
                    phaseState = Phase.DOWN
                }
            }
            Phase.DOWN -> {
                if (kneeMin < KNEE_BOTTOM){ // && drop > PELVIS_DROP && hipMin < 110.0) {
                    phaseState = Phase.BOTTOM
                    bottomEnterMs = nowMs
                }
                // 실수 방지: 다시 펴면 UP 복귀
                else if (kneeMin > KNEE_UP) {
                    phaseState = Phase.UP
                }
            }
            Phase.BOTTOM -> {
                // 충분히 바닥 유지했으면 올라가는중으로
                if (nowMs - bottomEnterMs >= MIN_BOTTOM_HOLD) {
                    phaseState = Phase.UP_GOING
                }
            }
            Phase.UP_GOING -> {
                val canCount = (kneeMin > KNEE_UP) && (drop < PELVIS_DROP*0.5)
                if (canCount) {
                    if (nowMs - lastRepMs >= MIN_REP_INTERVAL) {
                        count += 1
                        lastRepMs = nowMs
                    }
                    phaseState = Phase.UP
                }
                // 다시 너무 내려가면 바닥으로 복귀
                else if (kneeMin < KNEE_BOTTOM && drop > PELVIS_DROP) {
                    phaseState = Phase.BOTTOM
                    bottomEnterMs = nowMs
                }
            }
        }
    }
}