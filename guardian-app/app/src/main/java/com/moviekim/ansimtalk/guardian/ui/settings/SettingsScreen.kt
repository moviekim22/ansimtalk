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
import com.moviekim.ansimtalk.guardian.ui.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // TextField에 입력된 값을 저장하고 기억하는 변수
    var elderlyId by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("어르신 연결하기", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // 어르신 아이디 입력창
        OutlinedTextField(
            value = elderlyId,
            onValueChange = { elderlyId = it },
            label = { Text("연결할 어르신의 아이디를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 연결하기 버튼
        Button(
            onClick = {
                // 버튼 클릭 시 실행될 로직 (3단계에서 채울 부분)
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
            enabled = elderlyId.isNotBlank() // 아이디가 비어있지 않을 때만 버튼 활성화
        ) {
            Text("연결하기", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}