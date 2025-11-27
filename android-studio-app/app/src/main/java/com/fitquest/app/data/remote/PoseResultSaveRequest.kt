package com.fitquest.app.data.remote

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class PoseResultSaveRequest(

    @SerializedName("good_points")
    val good_points: String,

    @SerializedName("improvement_points")
    val improvement_points: String,

    @SerializedName("improvement_methods")
    val improvement_methods: String?,

    @SerializedName("created_at")
    val created_at: LocalDateTime,

    @SerializedName("image_base64")
    val image_base64: String
)
