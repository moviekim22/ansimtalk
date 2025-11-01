package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class UserLoginRequest(
    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String
)