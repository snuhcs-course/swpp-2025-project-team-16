package com.fitquest.app.model.pose

import com.google.gson.annotations.SerializedName

data class PoseAnalysis(
    val status: String,

    @SerializedName("good_points")
    val goodPoints: String,

    @SerializedName("improvement_points")
    val improvementPoints: String,

    @SerializedName("improvement_methods")
    val improvementMethods: String?
)