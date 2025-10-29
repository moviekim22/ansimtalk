package com.moviekim.ansimtalk.guardian.ui.api

import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionRequest
import com.moviekim.ansimtalk.guardian.ui.dto.LoginResponse
import com.moviekim.ansimtalk.guardian.ui.dto.UserLoginRequest
import com.moviekim.ansimtalk.guardian.ui.dto.UserSignUpRequest

interface ApiService {
    @POST("api/users/signup")
    fun signUp(@Body request: UserSignUpRequest): Call<Void>

    @POST("api/users/login")
    fun login(@Body request: UserLoginRequest): Call<LoginResponse> // 반환 타입을 LoginResponse로 변경

    @GET("api/connections")
    fun getMyConnections(): Call<List<ConnectionResponse>> // 여러 명일 수 있으므로 List로 받음

    @POST("api/connections")
    fun createConnection(@Body request: ConnectionRequest): Call<Void>
}
