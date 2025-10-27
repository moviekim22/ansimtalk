package com.moviekim.ansimtalk.guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviekim.ansimtalk.guardian.ui.home.HomeScreen
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianappTheme {
                HomeScreen()
            }
        }
    }
}