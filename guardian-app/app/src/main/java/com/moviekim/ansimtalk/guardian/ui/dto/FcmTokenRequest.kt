package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class FcmTokenRequest(
    @SerializedName("userId")
    val userId: Long,

    @SerializedName("fcmToken")
    val fcmToken: String
)