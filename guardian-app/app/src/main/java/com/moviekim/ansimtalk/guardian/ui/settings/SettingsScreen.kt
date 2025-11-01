package com.moviekim.ansimtalk.guardian.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.moviekim.ansimtalk.guardian.ui.api.RetrofitClient
import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionRequest
import com.moviekim.ansimtalk.guardian.ui.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var elderlyId by remember { mutableStateOf("") }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("어르신 연결하기", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = elderlyId,
            onValueChange = { elderlyId = it },
            label = { Text("연결할 어르신의 아이디를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val request = ConnectionRequest(elderlyLoginId = elderlyId)
                RetrofitClient.apiService.createConnection(request).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "성공적으로 연결되었습니다!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "연결에 실패했습니다. 아이디를 확인해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = elderlyId.isNotBlank()
        ) {
            Text("연결하기", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 로그아웃 버튼
        Button(
            onClick = {
                // 1. 저장된 로그인 정보 삭제
                sessionManager.clear()
                Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                // 2. 로그인 화면으로 이동 (이전 화면 스택 모두 제거)
                navController.navigate("login") {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("로그아웃", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(rememberNavController())
}
