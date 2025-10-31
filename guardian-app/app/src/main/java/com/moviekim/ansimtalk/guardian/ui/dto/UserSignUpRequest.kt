package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class UserSignUpRequest(
    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("role")
    val role: String
)