package com.moviekim.ansimtalk.guardian.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

// UI에서 사용할 색상들을 미리 정의합니다.
val EmergencyRed = Color(0xFFD32F2F)
val EmergencyRedLight = Color(0xFFFFEBEE)
val ActionGreen = Color(0xFF4CAF50)

// 긴급 상황 전체 화면 Composable
@Composable
fun EmergencyScreen(
    userName: String = "어머니(김순자)",
    onDismiss: () -> Unit // 화면을 닫기 위한 콜백 함수
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EmergencyRedLight // 전체 배경을 옅은 빨간색으로
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 긴급 상황 알림 바
            Header(userName = userName, onDismiss = onDismiss)

            // 지도 배경 (실제 지도는 여기에 통합해야 함)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 남은 공간을 모두 차지
                    .background(Color(0xFFE0E0E0)), // 임시 회색 배경
                contentAlignment = Alignment.Center
            ) {
                // 임시 위치 정보 카드
                LocationCard()
            }

            // 하단 액션 버튼 및 안내
            Footer()
        }
    }
}

@Composable
private fun Header(userName: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmergencyRed)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "긴급 상황",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "긴급 상황 발생!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
        }
        Text(
            "$userName 님이 도움을 요청했습니다",
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "신고 시간: 23:34",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LocationCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("현재 위치", fontWeight = FontWeight.Bold)
            Text("서울시 강남구 역삼동 123-45")
            Text("37.4979°N, 127.0276°E", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Footer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 119 신고 / 사용자 연락 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(
                text = "119 신고",
                secondaryText = "응급 구조 요청",
                color = EmergencyRed,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "사용자 연락",
                secondaryText = "직접 통화",
                color = ActionGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // 긴급 대응 안내 카드
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // 노란색 배경
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ⓘ 긴급 대응 안내", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("· 즉시 119에 신고하여 응급 구조를 요청하세요.", fontSize = 14.sp)
                Text("· 사용자에게 직접 연락하여 상황을 확인하세요.", fontSize = 14.sp)
                Text("· 위치 정보가 자동으로 119에 전송됩니다.", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    secondaryText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { /* TODO */ },
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontWeight = FontWeight.Bold, color = Color.White)
            Text(secondaryText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// Android Studio에서 미리보기를 위한 코드
@Preview(showBackground = true)
@Composable
fun EmergencyScreenPreview() {
    GuardianappTheme {
        EmergencyScreen(onDismiss = {})
    }
}