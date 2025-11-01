package com.moviekim.ansimtalk.guardian.ui.dto

import com.google.gson.annotations.SerializedName

data class ConnectionResponse(
    @SerializedName("id") // 연결 ID
    val id: Long,

    @SerializedName("elderlyId") // 어르신 ID
    val elderlyId: Long,

    @SerializedName("elderlyName") // 어르신 이름
    val elderlyName: String
)
