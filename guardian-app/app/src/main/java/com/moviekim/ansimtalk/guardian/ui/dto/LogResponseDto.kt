package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class LogResponseDto(
    @SerializedName("logId")
    val logId: Long,

    @SerializedName("date")
    val date: String,

    @SerializedName("isTaken")
    val isTaken: Boolean,

    @SerializedName("takenAt")
    val takenAt: String?,

    @SerializedName("medicationName")
    val medicationName: String,

    @SerializedName("scheduleTime")
    val scheduleTime: String
)
