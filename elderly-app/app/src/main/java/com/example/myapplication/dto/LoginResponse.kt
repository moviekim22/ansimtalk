package com.example.myapplication.dto;

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("name")
    val name: String
)
