package com.fitquest.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime


data class PoseAnalysis(
    @SerializedName("status")
    val status: String,

    @SerializedName("good_points")
    val good_points: String,

    @SerializedName("improvement_points")
    val improvement_points: String,

    @SerializedName("improvement_methods")
    val improvement_methods: String?
)