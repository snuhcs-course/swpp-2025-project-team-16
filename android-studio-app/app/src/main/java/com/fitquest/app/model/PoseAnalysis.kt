package com.fitquest.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class PoseAnalysis(
    val id: Int,
    val user: Int,
    val schedule: Int?,
    val session: Int?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("pose_data")
    val poseData: Map<String, Any>?,        // JSONField → Map

    @SerializedName("ai_comment")
    val aiComment: String?,

    @SerializedName("created_at")
    val createdAt: LocalDateTime? = null
)