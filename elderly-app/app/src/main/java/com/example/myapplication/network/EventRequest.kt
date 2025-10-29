package com.example.myapplication.network

data class EventRequest(
    val userId: Long,
    val eventType: String, // 서버의 Enum과 문자열로 맞춤
    val latitude: Double,
    val longitude: Double
)