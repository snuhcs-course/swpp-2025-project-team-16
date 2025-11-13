package com.fitquest.app.model

import com.google.gson.annotations.SerializedName

data class Feedback(
    val id: Int? = null,
    val user: Int? = null,
    val schedule: Int,

    @SerializedName("summary_text")
    val summaryText: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)
