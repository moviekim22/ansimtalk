package com.moviekim.ansimtalk.guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviekim.ansimtalk.guardian.ui.emergency.EmergencyScreen
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

class EmergencyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianappTheme {
                // 이 액티비티는 오직 EmergencyScreen만 보여줍니다.
                EmergencyScreen(onDismiss = { finish() })
            }
        }
    }
}