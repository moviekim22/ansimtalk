package com.moviekim.ansimtalk.guardian.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.moviekim.ansimtalk.guardian.ui.api.RetrofitClient
import com.moviekim.ansimtalk.guardian.ui.dto.FcmTokenRequest
import com.moviekim.ansimtalk.guardian.ui.dto.LoginResponse
import com.moviekim.ansimtalk.guardian.ui.dto.UserLoginRequest
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme
import com.moviekim.ansimtalk.guardian.ui.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("보호자 로그인", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = loginId,
            onValueChange = { loginId = it },
            label = { Text("아이디") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("자동 로그인")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val loginRequest = UserLoginRequest(loginId, password)
                RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.let { user ->
                                if (rememberMe) {
                                    sessionManager.saveUser(user)
                                }

                                // FCM 토큰 가져와서 서버로 전송
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (!task.isSuccessful) {
                                        Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                                        return@addOnCompleteListener
                                    }
                                    val token = task.result
                                    val tokenRequest = FcmTokenRequest(userId = user.id, fcmToken = token)

                                    RetrofitClient.apiService.updateFcmToken(tokenRequest).enqueue(object : Callback<Void> {
                                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                            if(response.isSuccessful) {
                                                Log.d("FCM", "FCM 토큰 서버에 성공적으로 등록")
                                            } else {
                                                Log.e("FCM", "FCM 토큰 서버 등록 실패")
                                            }
                                        }
                                        override fun onFailure(call: Call<Void>, t: Throwable) {
                                            Log.e("FCM", "FCM 토큰 등록 API 호출 실패", t)
                                        }
                                    })
                                }

                                Toast.makeText(context, "${user.name}님 환영합니다!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(context, "로그인 실패. 아이디 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("로그인", modifier = Modifier.padding(vertical = 8.dp))
        }

        TextButton(onClick = { navController.navigate("signup") }) {
            Text("회원가입")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    GuardianappTheme {
        LoginScreen(rememberNavController())
    }
}
