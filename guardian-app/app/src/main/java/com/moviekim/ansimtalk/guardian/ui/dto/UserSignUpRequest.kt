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
    val role: String // 서버의 Enum(Role)과 문자열로 맞춥
)