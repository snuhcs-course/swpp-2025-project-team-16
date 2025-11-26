package com.fitquest.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

data class DailySummary (
    val id: Int? = null,
    val user: Int? = null,
    val date: LocalDate,

    @SerializedName("summary_text")
    val summaryText: String? = null,

    @SerializedName("created_at")
    val createdAt: LocalDateTime? = null
)