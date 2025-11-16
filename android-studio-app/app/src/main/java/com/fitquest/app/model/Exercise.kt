package com.fitquest.app.model

data class Exercise(
    val emoji: String = "💪",
    val name: String = "",
    val done: String = "",
    val xp: String = "",
    val repDone:Int=0,
    val repTarget:Int=0,
    val accuracy: String = "",
    val duration: String = "",
    val detail: String = "",
    val status: String = "Ready",
    val id: String = "",
)
