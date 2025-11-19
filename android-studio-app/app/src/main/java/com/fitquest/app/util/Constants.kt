package com.fitquest.app.util

import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.model.Schedule
import com.fitquest.app.model.WorkoutItem
import org.threeten.bp.Duration
import org.threeten.bp.LocalTime

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

    fun formatExercisesSummary(item: DailyWorkoutItem): String {
        val exerciseNames = item.exercises.map { it.name }
        val counts = exerciseNames.groupingBy { it }.eachCount()
        return counts.entries.joinToString(", ") { (name, count) ->
            if (count > 1) "$name * $count set" else name
        }
    }

    fun formatExercisesSummary(item: DailyHistoryItem): String {
        val exerciseNames = item.exercises.map { it.activity }
        val counts = exerciseNames.groupingBy { it }.eachCount()
        return counts.entries.joinToString(", ") { (name, count) ->
            if (count > 1) "$name * $count set" else name
        }
    }

    fun calculateWorkoutItemXp(item: WorkoutItem): Int {
        val targetType = getTargetType(item.name)
        return when (targetType) {
            TargetType.REPS -> (item.targetCount ?: 0) * 10
            TargetType.DURATION -> item.targetDuration ?: 0
            else -> 0
        }
    }

    fun calculateScheduleTargetXp(schedule: Schedule): Int {
        val targetType = getTargetType(schedule.activity)
        return when (targetType) {
            TargetType.REPS -> (schedule.repsTarget ?: 0) * 10
            TargetType.DURATION -> schedule.durationTarget ?: 0
            else -> 0
        }
    }

    fun calculateScheduleEarnedXp(schedule: Schedule): Int {
        val targetType = getTargetType(schedule.activity)
        return when (targetType) {
            TargetType.REPS -> (schedule.repsDone ?: 0) * 10
            TargetType.DURATION -> schedule.durationDone ?: 0
            else -> 0
        }
    }

    fun calculateDailyWorkoutTotalXp(items: List<WorkoutItem>): Int {
        return items.sumOf { calculateWorkoutItemXp(it) }
    }

    fun calculateDailyHistoryTotalTargetXp(schedules: List<Schedule>): Int {
        return schedules.sumOf { calculateScheduleTargetXp(it) }
    }

    fun calculateDailyHistoryTotalEarnedXp(schedules: List<Schedule>): Int {
        return schedules.sumOf { calculateScheduleEarnedXp(it) }
    }

    fun calculateScheduleCompletionPercent(ex: Schedule): Int {
        val targetType = getTargetType(ex.activity)

        val done: Int?
        val target: Int?

        when (targetType) {
            TargetType.REPS -> {
                done = ex.repsDone
                target = ex.repsTarget
            }
            TargetType.DURATION -> {
                done = ex.durationDone
                target = ex.durationTarget
            }
            else -> return 0
        }

        return if (done != null && target != null && target > 0) {
            (done * 100) / target
        } else {
            0
        }
    }

    fun calculateDailyHistoryAverageCompletion(schedules: List<Schedule>): Int {
        if (schedules.isEmpty()) return 0

        val totalPercent = schedules.sumOf { calculateScheduleCompletionPercent(it) }

        return totalPercent / schedules.size
    }

    fun calculateScheduleDuration(schedule: Schedule): Int {
        val seconds = try {
            val start = LocalTime.parse(schedule.startTime)
            val end = LocalTime.parse(schedule.endTime)
            Duration.between(start, end).seconds.toInt()
        } catch (e: Exception) {
            0
        }
        return seconds / 60
    }

    fun calculateDailyHistoryTotalDuration(schedules: List<Schedule>): Int {
        val totalMinutes = schedules.sumOf { calculateScheduleDuration(it) }
        return totalMinutes
    }
}
