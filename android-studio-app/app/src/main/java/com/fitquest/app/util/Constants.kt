package com.fitquest.app.util

enum class TargetType {
    REPS,
    DURATION
}

data class ActivityMetadata(
    val label: String,
    val emoji: String,
    val targetType: TargetType
)

object ActivityUtils {
    // Key: 소문자 활동 이름
    val activityMetadataMap = mapOf(
        "squat" to ActivityMetadata(
            label = "Squat",
            emoji = "\uD83C\uDFCB\u200D\u2642\uFE0F", // 🏋️‍♂️
            targetType = TargetType.REPS
        ),
        "plank" to ActivityMetadata(
            label = "Plank",
            emoji = "\uD83E\uDDD8\u200D\u2642\uFE0F", // 🧘‍♂️
            targetType = TargetType.DURATION
        ),
        "lunge" to ActivityMetadata(
            label = "Lunge",
            emoji = "\uD83E\uDDB5", // 🦵
            targetType = TargetType.REPS
        )
    )

    fun getLabel(activity: String) =
        activityMetadataMap[activity.lowercase()]?.label ?: activity

    fun getEmoji(activity: String) =
        activityMetadataMap[activity.lowercase()]?.emoji ?: ""

    fun getTargetType(activity: String): TargetType? =
        activityMetadataMap[activity.lowercase()]?.targetType
}
