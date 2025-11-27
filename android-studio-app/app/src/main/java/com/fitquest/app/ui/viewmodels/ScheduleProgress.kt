package com.fitquest.app.ui.viewmodels

sealed class ScheduleProgress {
    data class Step(val step: Int, val message: String) : ScheduleProgress()
    object Completed : ScheduleProgress()
    data class Error(val error: String) : ScheduleProgress()
}
