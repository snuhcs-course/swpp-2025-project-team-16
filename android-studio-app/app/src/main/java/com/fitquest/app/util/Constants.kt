package com.fitquest.app.util
object ActivityUtils {
    val labelMap = mapOf(
        "squat" to "Squat",
        "pushup" to "Push-Up",
        "plank" to "Plank"
    )

    val emojiMap = mapOf(
        "squat" to "\uD83C\uDFCB\u200D\u2642\uFE0F",  // 🏋️‍♂️
        "pushup" to "\uD83D\uDCAA",                  // 💪
        "plank" to "\uD83E\uDDD8\u200D\u2642\uFE0F" // 🧘‍♂️
    )

    fun getLabel(activity: String) = labelMap[activity.lowercase()] ?: activity
    fun getEmoji(activity: String) = emojiMap[activity.lowercase()] ?: ""
}
