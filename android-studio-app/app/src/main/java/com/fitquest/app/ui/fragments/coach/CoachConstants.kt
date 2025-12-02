package com.fitquest.app.ui.fragments.coach

object CoachConstants {
    // Countdown
    const val COUNTDOWN_DURATION_SECONDS = 10
    const val COUNTDOWN_INTERVAL_MS = 1000L

    // Tracking lock FSM
    const val VIS_THRESH = 0.95f
    val LOWER_NEEDED = intArrayOf(23, 24, 25, 26, 27, 28)
    const val LOWER_REQUIRED = 5
    const val BAD_VIS_LIMIT = 12
    const val GOOD_VIS_LIMIT = 12
    const val DISARM_MS_AFTER_UNLOCK = 300L

    // Messages
    const val COACH_MSG_IDLE = "Position yourself in frame"
    const val COACH_MSG_ANALYZING = "Analyzing form... 🔍"
    const val COACH_MSG_READY = "Get ready... ⏳"
    const val COACH_MSG_STEP_BACK = "STEP BACK"

    // Photo capture
    const val MIN_CAPTURE_INTERVAL_MS = 3000L
    const val PLANK_CAPTURE_INTERVAL_MS = 15000L
    const val FILE_PREFIX_BOTTOM_REP = "aicoach_bottom_rep_"
    const val FILE_EXTENSION = ".jpg"

    // Rep popup animation
    const val REP_POPUP_DURATION_MS = 600L
    const val REP_POPUP_SCALE = 1.4f

    // Arguments
    const val ARG_SCHEDULE_ID = "scheduleId"
    const val ARG_ACTIVITY_KEY = "activityKey"
    const val ARG_REPS_TARGET = "repsTarget"
    const val ARG_DURATION_TARGET = "durationTarget"
}