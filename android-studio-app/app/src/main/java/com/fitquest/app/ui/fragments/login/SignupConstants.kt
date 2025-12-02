package com.fitquest.app.ui.fragments.login

object SignupConstants {
    // Countdown
    const val COUNTDOWN_DURATION_SECONDS = 10
    const val COUNTDOWN_INTERVAL_MS = 1000L

    // Tracking lock FSM (CoachConstants와 동일)
    const val VIS_THRESH = 0.95f
    val LOWER_NEEDED = intArrayOf(23, 24, 25, 26, 27, 28)
    const val LOWER_REQUIRED = 5
    const val BAD_VIS_LIMIT = 12
    const val GOOD_VIS_LIMIT = 12
    const val DISARM_MS_AFTER_UNLOCK = 300L

    // Messages
    const val MSG_STEP_BACK = "STEP BACK"

    // Logging (optional)
    const val LOG_INTERVAL_MS = 0L // 0 = every frame
}