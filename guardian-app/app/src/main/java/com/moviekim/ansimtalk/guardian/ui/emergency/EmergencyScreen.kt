package com.moviekim.ansimtalk.guardian.ui.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.moviekim.ansimtalk.guardian.ui.dto.EventType
import java.util.Locale

// 메인 화면
@Composable
fun EmergencyScreen(
    eventType: EventType,
    elderlyName: String,
    reportTime: String,
    address: String,
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onRoute: () -> Unit,
    onCall119: () -> Unit,
    onCallUser: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            EmergencyHeader(eventType, elderlyName, reportTime, onDismiss)
            EmergencyMap(address, latitude, longitude, onRoute)
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(onCall119, onCallUser)
            Spacer(modifier = Modifier.height(16.dp))
            GuidanceInfo()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 1. 상단 헤더
@Composable
private fun EmergencyHeader(eventType: EventType, elderlyName: String, reportTime: String, onDismiss: () -> Unit) {
    val title = when (eventType) {
        EventType.EMERGENCY_CALL -> "긴급 호출 발생!"
        EventType.FALL_DETECTED -> "낙상 감지 발생!"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE53935)) // Red 600
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = "긴급 상황 아이콘", tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("$elderlyName 님이 도움을 요청했습니다", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("신고 시간: $reportTime", color = Color.White, fontSize = 14.sp)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
        }
    }
}

// 2. 지도 영역
@Composable
private fun EmergencyMap(address: String, latitude: Double, longitude: Double, onRoute: () -> Unit) {
    val location = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.HYBRID)
        ) {
            Marker(
                state = MarkerState(position = location),
                title = "긴급 상황 발생 위치"
            )
        }
        // 현재 위치 정보 카드
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "현재 위치 아이콘", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("현재 위치", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(address, fontSize = 14.sp, color = Color.DarkGray)
                Text(String.format(Locale.US, "%.4f°N, %.4f°E", latitude, longitude), fontSize = 12.sp, color = Color.Gray)
            }
        }
        // 길찾기 버튼
        FloatingActionButton(
            onClick = onRoute,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Navigation, contentDescription = "길찾기", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("길찾기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 3. 주요 액션 버튼 (119, 사용자 연락)
@Composable
private fun ActionButtons(onCall119: () -> Unit, onCallUser: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButton(text = "119 신고", subText = "응급 구조 요청", icon = Icons.Default.Call, color = Color(0xFFD32F2F), modifier = Modifier.weight(1f), onClick = onCall119)
        ActionButton(text = "사용자 연락", subText = "직접 통화", icon = Icons.Default.Call, color = Color(0xFF388E3C), modifier = Modifier.weight(1f), onClick = onCallUser)
    }
}

@Composable
private fun ActionButton(text: String, subText: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

// 4. 긴급 대응 안내
@Composable
private fun GuidanceInfo() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // Light Yellow
        border = BorderStroke(1.dp, Color(0xFFFFEB3B)) // Yellow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "안내 아이콘", tint = Color(0xFFFBC02D))
                Spacer(modifier = Modifier.width(8.dp))
                Text("긴급 대응 안내", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("• 즉시 119에 신고하여 응급 구조를 요청하세요", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("• 사용자에게 직접 연락하여 상황을 확인하세요", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("• 위치 정보가 자동으로 119에 전송됩니다", fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}
