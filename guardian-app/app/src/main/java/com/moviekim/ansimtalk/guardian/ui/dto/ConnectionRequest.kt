package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class ConnectionRequest(
    @SerializedName("elderlyLoginId")
    val elderlyLoginId: String
)