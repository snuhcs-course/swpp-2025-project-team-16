package com.fitquest.app.ui.coachutils.counter

/**
 * BaseCounter (Strategy 역할)
 * - 포즈 랜드마커 좌표를 받아 반복 횟수/단계 등을 갱신하는 공통 인터페이스.
 *
 * points: MediaPipe NormalizedLandmark 기준 (0~1 정규화, y는 화면 아래로 증가)
 * nowMs : System.currentTimeMillis()
 */
abstract class BaseCounter {
    /** 현재 반복 횟수 (또는 기준이 되는 정수 카운트) */
    abstract var count: Int

    /** 현재 운동 단계 표시 (예: UP/DOWN/BOTTOM/HOLDING 등) */
    abstract var phase: String

    /**
     * 프레임 단위 업데이트
     * @param points float[33*3] = [x,y,z, x,y,z, ...]
     * @param nowMs  System.currentTimeMillis()
     */
    abstract fun update(points: FloatArray, nowMs: Long)

    /** 카운터/상태 초기화 */
    abstract fun reset(nowMs: Long)

    // -------- Strategy 패턴을 위한 공통 훅들 --------

    /**
     * UI에 보여줄 카운트 텍스트.
     * - 기본: 정수 count 그대로
     * - 플랭크: 소수 1자리 초 등으로 override 가능
     */
    open fun getDisplayText(): String = count.toString()

    /**
     * 이번 카운트 기준으로 줄 XP 포인트.
     * - 기본: count * 10
     * - 운동별로 다르게 주고 싶으면 override
     */
    open fun getXpPoints(): Int = count * 10

    /**
     * 이번 프레임에서 REP 팝업/축하 애니메이션을 띄워야 하는지 여부.
     * - 기본: 트레이닝 중이고, count가 증가했을 때만 true
     * - 플랭크처럼 ‘초당 팝업이 시끄러운’ 운동은 false로 override
     */
    open fun shouldShowRepPopup(previousCount: Int, isTraining: Boolean): Boolean {
        return isTraining && count > previousCount
    }
}
