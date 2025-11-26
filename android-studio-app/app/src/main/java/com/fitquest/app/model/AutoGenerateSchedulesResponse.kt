package com.fitquest.app.model

import com.google.gson.annotations.SerializedName

data class AutoGenerateSchedulesResponse(
    @SerializedName("created_count")
    val createdCount: Int,

    @SerializedName("dates_generated")
    val datesGenerated: List<String>
)