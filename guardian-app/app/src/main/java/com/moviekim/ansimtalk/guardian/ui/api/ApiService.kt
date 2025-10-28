package com.moviekim.ansimtalk.guardian.ui.api

import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionRequest

interface ApiService {
    @GET("api/connections")
    fun getMyConnections(): Call<List<ConnectionResponse>> // 여러 명일 수 있으므로 List로 받음

    @POST("api/connections")
    fun createConnection(@Body request: ConnectionRequest): Call<Void>
}