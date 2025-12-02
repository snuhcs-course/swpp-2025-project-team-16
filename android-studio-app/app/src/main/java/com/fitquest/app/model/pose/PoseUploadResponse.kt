package com.fitquest.app.model.pose

import com.google.gson.annotations.SerializedName

data class PoseUploadResponse(
    val id: Int? = null,

    @SerializedName("good_points")
    val goodPoints: String? = null,

    @SerializedName("improvement_points")
    val improvementPoints: String? = null,

    @SerializedName("improvement_methods")
    val improvementMethods: String? = null
)