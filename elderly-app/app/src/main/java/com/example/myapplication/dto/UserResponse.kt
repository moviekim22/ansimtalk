package com.example.myapplication.dto

data class UserResponse(
    val success: Boolean,
    val message: String,
    val data: UserInfo
)
