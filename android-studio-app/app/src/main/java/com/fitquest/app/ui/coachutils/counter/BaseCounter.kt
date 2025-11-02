package com.fitquest.app.ui.coachutils.counter

/**
 * BaseCounter
 * - 포즈 랜드마커의 (정규화) 랜드마크 좌표 [x,y,z,...] 를 받아
 *   반복 횟수/단계 등을 갱신하는 공통 인터페이스.
 *
 * points: MediaPipe NormalizedLandmark 기준 (0~1 정규화, y는 화면 아래로 증가)
 * nowMs : System.currentTimeMillis()
 */
abstract class BaseCounter {
    /** 현재 반복 횟수 */
    abstract var count: Int

    /** 현재 운동 단계 표시 (예: UP/DOWN/BOTTOM 등) */
    abstract var phase: String

    /**
     * 프레임 단위 업데이트
     * @param points float[33*3] = [x,y,z, x,y,z, ...]
     * @param nowMs  System.currentTimeMillis()
     */
    abstract fun update(points: FloatArray, nowMs: Long)

    /** 카운터/상태 초기화 */
    abstract fun reset(nowMs: Long)
}
