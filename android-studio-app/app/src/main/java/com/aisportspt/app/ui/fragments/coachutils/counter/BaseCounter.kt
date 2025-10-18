package com.aisportspt.app.ui.fragments.coachutils.counter

abstract class BaseCounter {
    /** 현재 반복 횟수 */
    abstract var count: Int

    /** 현재 운동 단계 (enum or string 모두 가능) */
    abstract var phase: String

    /**
     * 프레임 단위로 자세 데이터를 업데이트하는 함수
     * @param points MediaPipe PoseLandmarker의 world landmark 좌표 (x,y,z 반복)
     * @param nowMs 현재 프레임의 시간 (System.currentTimeMillis)
     */
    abstract fun update(points: FloatArray, nowMs: Long)

    /**
     * 초기화 함수
     * @param nowMs 현재 시간
     */
    abstract fun reset(nowMs: Long)
}
