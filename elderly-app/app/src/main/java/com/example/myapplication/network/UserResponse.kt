package com.example.myapplication.network

// 실제 서버는 보통 이런 식으로 데이터를 한 번 더 감싸서 보냅니다.
data class UserResponse(
    val success: Boolean,
    val data: UserInfo,
    val message: String?
)