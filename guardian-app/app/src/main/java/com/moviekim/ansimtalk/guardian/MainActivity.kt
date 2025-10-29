package com.moviekim.ansimtalk.guardian

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.moviekim.ansimtalk.guardian.ui.auth.LoginScreen
import com.moviekim.ansimtalk.guardian.ui.auth.SignUpScreen
import com.moviekim.ansimtalk.guardian.ui.home.HomeScreen
import com.moviekim.ansimtalk.guardian.ui.settings.SettingsScreen
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

class MainActivity : ComponentActivity() {

    private val TAG = "FCM_Main"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "현재 FCM 토큰: $token")
        }

        enableEdgeToEdge()
        setContent {
            GuardianappTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        // 로그인/회원가입 화면에서는 바텀 네비게이션 숨기기
                        if (currentRoute != "login" && currentRoute != "signup") {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(navController)
                        }
                        composable("signup") {
                            SignUpScreen(navController)
                        }
                        composable("home") {
                            HomeScreen()
                        }
                        composable("history") {
                            // TODO: 기록 화면 구현
                        }
                        composable("settings") {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        "home" to Icons.Default.Home,
        "history" to Icons.Default.List,
        "settings" to Icons.Default.Settings
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.second, contentDescription = screen.first) },
                label = { Text(screen.first) },
                selected = currentRoute == screen.first,
                onClick = {
                    navController.navigate(screen.first) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}