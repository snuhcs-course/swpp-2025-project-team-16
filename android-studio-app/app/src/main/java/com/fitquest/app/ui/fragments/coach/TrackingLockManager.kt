package com.fitquest.app.ui.fragments.coach

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.exp

class TrackingLockManager {
    private var trackingLocked = false
    private var badVisFrames = 0
    private var goodVisFrames = 0
    private var disarmUntilMs = 0L

    fun isLocked(): Boolean = trackingLocked

    fun shouldDisarm(currentTimeMs: Long): Boolean = currentTimeMs < disarmUntilMs

    fun updateLockState(
        landmarks: List<NormalizedLandmark>,
        currentTimeMs: Long
    ): LockStateChange {
        val lowerCnt = lowerBodyVisibleCount(landmarks)
        val visGood = (lowerCnt >= CoachConstants.LOWER_REQUIRED)

        if (trackingLocked) {
            if (visGood) {
                goodVisFrames++
                if (goodVisFrames >= CoachConstants.GOOD_VIS_LIMIT) {
                    trackingLocked = false
                    goodVisFrames = 0
                    badVisFrames = 0
                    disarmUntilMs = currentTimeMs + CoachConstants.DISARM_MS_AFTER_UNLOCK
                    return LockStateChange.UNLOCKED
                }
            } else {
                goodVisFrames = 0
            }
            return LockStateChange.STILL_LOCKED
        } else {
            if (!visGood) {
                badVisFrames++
                if (badVisFrames >= CoachConstants.BAD_VIS_LIMIT) {
                    trackingLocked = true
                    badVisFrames = 0
                    goodVisFrames = 0
                    return LockStateChange.LOCKED
                }
            } else {
                badVisFrames = 0
            }
            return LockStateChange.NO_CHANGE
        }
    }

    fun reset() {
        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
    }

    private fun lowerBodyVisibleCount(landmarks: List<NormalizedLandmark>): Int {
        var ok = 0
        for (i in CoachConstants.LOWER_NEEDED) {
            val s = safeVis(landmarks[i])
            if (s >= CoachConstants.VIS_THRESH) ok++
        }
        return ok
    }

    private fun safeVis(landmark: NormalizedLandmark): Float {
        val visProb = toProbMaybeLogit((landmark.visibility() as? Number)?.toFloat())
        val presProb = toProbMaybeLogit((landmark.presence() as? Number)?.toFloat())
        val best = listOfNotNull(visProb, presProb).maxOrNull()
        if (best != null) return best.coerceIn(0f, 1f)
        return if (inFrame(landmark)) 1f else 0f
    }

    private fun toProbMaybeLogit(x: Float?): Float? {
        if (x == null || x.isNaN()) return null
        return if (x in 0f..1f) x else 1f / (1f + exp(-x))
    }

    private fun inFrame(landmark: NormalizedLandmark): Boolean {
        val x = landmark.x()
        val y = landmark.y()
        return x in 0f..1f && y in 0f..1f
    }

    enum class LockStateChange {
        LOCKED,
        UNLOCKED,
        STILL_LOCKED,
        NO_CHANGE
    }
}