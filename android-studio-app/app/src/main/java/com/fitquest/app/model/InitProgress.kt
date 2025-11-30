package com.fitquest.app.model

sealed class InitProgress {
    data class Step(val step: Int, val message: String) : InitProgress()
    object Completed : InitProgress()
    data class Error(val error: String) : InitProgress()
}