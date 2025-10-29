package com.moviekim.ansimtalk.guardian.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme
import com.moviekim.ansimtalk.guardian.ui.util.SessionManager
import java.text.SimpleDateFormat
import java.util.*

val StatusGreen = Color(0xFF4CAF50)
val StatusRed = Color(0xFFD32F2F)
val StatusGray = Color(0xFFBDBDBD)
val BackgroundGray = Color(0xFFF5F5F5)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userName = sessionManager.getUserName() ?: "보호자"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(userName = userName)
        Spacer(modifier = Modifier.height(8.dp))
        CurrentStatusCard(isSafe = true)
        QuickContactCard()
        DailyCheckInCard(isChecked = true)
        MedicationCard()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(userName: String) {
    val currentDate = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.getDefault()).format(Date())

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "${userName}님",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = currentDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun CurrentStatusCard(isSafe: Boolean) {
    val statusColor = if (isSafe) StatusGreen else StatusRed
    val statusIcon = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning
    val statusText = if (isSafe) "현재 안전합니다" else "확인이 필요합니다"

    InfoCard(
        icon = statusIcon,
        iconTint = statusColor,
        title = statusText,
        titleColor = statusColor
    ) {
        Text(
            "마지막 활동: 15분 전",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun QuickContactCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContactButton(icon = Icons.Default.Call, text = "전화 걸기")
            ContactButton(icon = Icons.Default.LocationOn, text = "위치 확인")
        }
    }
}

@Composable
fun ContactButton(icon: ImageVector, text: String) {
    Button(
        onClick = { /*TODO*/ },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = null
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, fontSize = 14.sp)
        }
    }
}

@Composable
fun DailyCheckInCard(isChecked: Boolean) {
    InfoCard(icon = Icons.Default.Check, title = "오늘의 안부 확인") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("마지막 확인: 오전 9:30", color = Color.Gray, fontSize = 14.sp)
            StatusChip(
                text = if (isChecked) "확인 완료" else "미확인",
                isCompleted = isChecked
            )
        }
    }
}

@Composable
fun MedicationCard() {
    InfoCard(icon = Icons.Default.Info, title = "오늘의 약 복용") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MedicationItem(time = "오전 8:00", name = "아침 약", isTaken = true)
            MedicationItem(time = "오후 12:00", name = "점심 약", isTaken = true)
            MedicationItem(time = "오후 7:00", name = "저녁 약", isTaken = false)
        }
    }
}

@Composable
fun MedicationItem(time: String, name: String, isTaken: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontWeight = FontWeight.Bold)
            Text(time, color = Color.Gray, fontSize = 12.sp)
        }
        StatusChip(
            text = if (isTaken) "복용 완료" else "복용 전",
            isCompleted = isTaken
        )
    }
}

@Composable
fun StatusChip(text: String, isCompleted: Boolean) {
    val backgroundColor = if (isCompleted) StatusGreen else StatusGray
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = Color.Black,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GuardianappTheme {
        HomeScreen()
    }
}
