package com.moviekim.ansimtalk.guardian

import android.os.Bundle
import android.util.Log // <-- 추가
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.messaging.FirebaseMessaging // <-- 추가
import com.moviekim.ansimtalk.guardian.ui.home.HomeScreen
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

class MainActivity : ComponentActivity() {

    private val TAG = "FCM_Main" // 로그 태그 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ▼▼▼▼▼ 앱이 켜질 때마다 현재 토큰을 가져오는 코드 추가 ▼▼▼▼▼
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }
            // 현재 토큰 가져오기 성공!
            val token = task.result
            Log.d(TAG, "현재 FCM 토큰: $token")
        }
        // ▲▲▲▲▲ 여기까지 추가 ▲▲▲▲▲

        enableEdgeToEdge()
        setContent {
            GuardianappTheme {
                HomeScreen()
            }
        }
    }
}