package com.fitquest.app.ui.fragments.pose

object PoseConstants {
    const val REQUEST_CODE_PERMISSIONS = 10

    // Camera settings
    const val DEFAULT_EXERCISE = "squat"

    // Countdown
    const val COUNTDOWN_DURATION_MS = 10_000L
    const val COUNTDOWN_INTERVAL_MS = 1_000L
    const val COUNTDOWN_INITIAL_SECONDS = 10

    // Loading progress
    const val LOADING_DURATION_MS = 60_000L
    const val LOADING_INTERVAL_MS = 1_000L
    const val LOADING_TARGET_PROGRESS = 90
    const val LOADING_MAX_PROGRESS = 100

    // Image processing
    const val IMAGE_MAX_SIDE_PX = 720
    const val IMAGE_JPEG_QUALITY = 85

    // File naming
    const val FILE_PREFIX_CAMERA = "pose_"
    const val FILE_PREFIX_GALLERY = "gallery_"
    const val FILE_PREFIX_PROCESSED = "pose_processed_"
    const val FILE_EXTENSION = ".jpg"

    // Permissions
    val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
}