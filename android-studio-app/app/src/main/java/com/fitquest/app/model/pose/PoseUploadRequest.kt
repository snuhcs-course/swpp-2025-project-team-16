package com.fitquest.app.model.pose

import com.google.gson.annotations.SerializedName

data class PoseUploadRequest(
    val category: String,

    @SerializedName("image_base64")
    val imageBase64: String
)