package com.example.myapplication.dto

import com.google.gson.annotations.SerializedName

data class UserLoginRequest(
    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("password")
    val password: String
)