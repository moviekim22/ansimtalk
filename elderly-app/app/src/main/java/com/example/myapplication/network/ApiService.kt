package com.example.myapplication.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET // GET 추가
import retrofit2.http.POST
import retrofit2.http.Path // Path 추가

interface ApiService {
    @POST("api/events")
    fun sendEvent(@Body request: EventRequest): Call<String>
    // 안부 전송 API (기존)
    @POST("status")
    fun sendStatus(@Body statusRequest: StatusRequest): Call<Void>

    // --- 사용자 정보 조회 API (새로 추가) ---
    // "/user/1" 과 같은 경로로 GET 요청을 보냅니다.
    @GET("user/{userId}")
    fun getUserInfo(@Path("userId") userId: Long): Call<UserResponse>
}
