package com.fitquest.app.util
object ActivityUtils {
    val labelMap = mapOf(
        "squat" to "Squat",
        "plank" to "Plank",
        "lunge" to "Lunge"
    )

    val emojiMap = mapOf(
        "squat" to "\uD83C\uDFCB\u200D\u2642\uFE0F",  // 🏋️‍♂️
        "plank" to "\uD83E\uDDD8\u200D\u2642\uFE0F", // 🧘‍♂️
        "lunge" to "\uD83E\uDDB5" // 🦵
    )

    fun getLabel(activity: String) = labelMap[activity.lowercase()] ?: activity
    fun getEmoji(activity: String) = emojiMap[activity.lowercase()] ?: ""
}
