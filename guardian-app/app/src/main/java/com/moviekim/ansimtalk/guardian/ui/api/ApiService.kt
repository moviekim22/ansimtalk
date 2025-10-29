package com.moviekim.ansimtalk.guardian.ui.api

import com.moviekim.ansimtalk.guardian.ui.dto.* 
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {
    @POST("api/users/signup")
    fun signUp(@Body request: UserSignUpRequest): Call<Void>

    @POST("api/users/login")
    fun login(@Body request: UserLoginRequest): Call<LoginResponse>

    @PUT("api/users/fcm-token")
    fun updateFcmToken(@Body request: FcmTokenRequest): Call<Void>

    @GET("api/connections")
    fun getMyConnections(): Call<List<ConnectionResponse>>

    @POST("api/connections")
    fun createConnection(@Body request: ConnectionRequest): Call<Void>
}