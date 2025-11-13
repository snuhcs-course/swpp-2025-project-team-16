package com.fitquest.app.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Schedule(
    val id: Int? = null,
    val user: Int? = null,

    @SerializedName("scheduled_date")
    val scheduledDate: String,

    @SerializedName("start_time")
    val startTime: String,

    @SerializedName("end_time")
    val endTime: String,

    val activity: String,

    @SerializedName("reps_target")
    val repsTarget: Int? = null,

    @SerializedName("reps_done")
    val repsDone: Int? = null,

    @SerializedName("duration_target")
    val durationTarget: Int? = null,

    @SerializedName("duration_done")
    val durationDone: Int? = null,

    val status: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Parcelable
