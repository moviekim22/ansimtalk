package com.example.myapplication.network

import com.example.myapplication.dto.EventRequest
import com.example.myapplication.dto.LoginResponse
import com.example.myapplication.dto.UserLoginRequest
import com.example.myapplication.dto.UserResponse
import com.example.myapplication.dto.UserSignUpRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/users/signup")
    fun signUp(@Body request: UserSignUpRequest): Call<Void>

    @POST("api/users/login")
    fun login(@Body request: UserLoginRequest): Call<LoginResponse>

    @POST("status")
    fun sendStatus(@Body statusRequest: StatusRequest): Call<Void>

    @GET("api/users/{userId}")
    fun getUserInfo(@Path("userId") userId: Long): Call<UserResponse>

    @POST("api/events/emergency")
    fun sendEmergencyEvent(@Body eventRequest: EventRequest): Call<Void>
}
