package com.moviekim.ansimtalk.guardian.ui.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 앱 전체에서 단 하나만 존재해야 하는 Retrofit 사무실 (Singleton)
object RetrofitClient {

    // 💡 중요: '10.0.2.2'는 안드로이드 에뮬레이터가
    // 개발용 PC(localhost)를 가리킬 때 사용하는 특수 IP 주소입니다.
    // 시연 시 실제 폰을 사용할 때는 서버 PC의 내부 IP로 변경해야 합니다.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // 통신 과정을 로그로 보여주는 로깅 인터셉터 (디버깅 필수!)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient에 로깅 인터셉터 추가
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit 객체 생성
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Gson을 사용해 JSON 파싱
            .build()
    }

    // ApiService 인터페이스의 구현체를 생성
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}