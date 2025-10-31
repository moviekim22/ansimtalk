package com.example.myapplication.dto

data class EventRequest(
    val userId: Long,
    val eventType: EventType,
    val latitude: Double,
    val longitude: Double
)
