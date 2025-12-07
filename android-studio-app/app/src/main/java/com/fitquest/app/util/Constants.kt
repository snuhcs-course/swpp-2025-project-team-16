package com.fitquest.app.util

import com.fitquest.app.model.Schedule
import com.fitquest.app.model.Session

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
    // Key: lowercase activity name
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

    fun formatActivitiesSummary(
        schedules: List<Schedule>,
        sessions: List<Session> = emptyList()
    ): String {

        val activityCountMap = mutableMapOf<String, Int>()

        schedules.forEach { schedule ->
            val key = schedule.activity
            activityCountMap[key] = (activityCountMap[key] ?: 0) + 1
        }

        sessions.forEach { session ->
            val key = session.activity
            activityCountMap[key] = (activityCountMap[key] ?: 0) + 1
        }

        return activityCountMap.entries.joinToString(", ") { (activity, count) ->
            if (count > 1) "$activity x $count" else activity
        }
    }

    fun calculateXp(targetType: TargetType?, actualValue: Int?): Int {
        val value = actualValue ?: 0
        return when (targetType) {
            TargetType.REPS -> value * 10
            TargetType.DURATION -> value
            else -> 0
        }
    }

    fun calculateXp(activity: String, actualValue: Int?): Int {
        val targetType = getTargetType(activity)
        return calculateXp(targetType, actualValue)
    }

    fun calculateTotalEarnedXpForSchedules(schedules: List<Schedule>): Int {
        return schedules.sumOf { calculateEarnedXpForSchedule(it) }
    }

    fun calculateTotalEarnedXpForSessions(sessions: List<Session>): Int {
        return sessions.sumOf { calculateEarnedXpForSession(it) }
    }

    fun calculateTargetXp(schedule: Schedule): Int {
        val targetType = getTargetType(schedule.activity)
        val actualValue = if (targetType == TargetType.REPS) schedule.repsTarget else schedule.durationTarget
        return calculateXp(targetType, actualValue)
    }

    fun calculateTotalTargetXp(schedules: List<Schedule>): Int {
        return schedules.sumOf { calculateTargetXp(it) }
    }

    fun calculateEarnedXpForSchedule(schedule: Schedule): Int {
        val targetType = getTargetType(schedule.activity)
        val actualValue = if (targetType == TargetType.REPS) schedule.repsDone else schedule.durationDone
        return calculateXp(targetType, actualValue)
    }

    fun calculateEarnedXpForSession(session: Session): Int {
        val targetType = getTargetType(session.activity)
        val actualValue = if (targetType == TargetType.REPS) session.repsCount else session.duration
        return calculateXp(targetType, actualValue)
    }

    fun calculateTotalEarnedXp(
        schedules: List<Schedule>,
        sessions: List<Session> = emptyList()
    ): Int {
        return calculateTotalEarnedXpForSchedules(schedules) + calculateTotalEarnedXpForSessions(sessions)
    }

    fun calculateCompletionPercent(schedule: Schedule): Int {
        val targetType = getTargetType(schedule.activity)

        val done: Int?
        val target: Int?

        when (targetType) {
            TargetType.REPS -> {
                done = schedule.repsDone
                target = schedule.repsTarget
            }
            TargetType.DURATION -> {
                done = schedule.durationDone
                target = schedule.durationTarget
            }
            else -> return 0
        }

        return if (done != null && target != null && target > 0) {
            (done * 100) / target
        } else {
            0
        }
    }

    fun calculateAverageCompletionPercent(schedules: List<Schedule>): Int {
        if (schedules.isEmpty()) return 0

        val totalPercent = schedules.sumOf { calculateCompletionPercent(it) }

        return totalPercent / schedules.size
    }
}
