package com.fitquest.app.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

@Parcelize
data class Schedule(
    val id: Int? = null,
    val user: Int? = null,

    @SerializedName("scheduled_date")
    val scheduledDate: LocalDate,

    @SerializedName("start_time")
    val startTime: LocalTime,

    @SerializedName("end_time")
    val endTime: LocalTime,

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
    val createdAt: LocalDateTime? = null,

    @SerializedName("updated_at")
    val updatedAt: LocalDateTime? = null
) : Parcelable
