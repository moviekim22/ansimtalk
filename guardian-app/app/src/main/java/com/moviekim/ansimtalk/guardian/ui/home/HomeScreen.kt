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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme

// UI에서 사용할 색상들을 미리 정의합니다.
val StatusGreen = Color(0xFF4CAF50)
val StatusRed = Color(0xFFD32F2F)
val StatusGray = Color(0xFFBDBDBD)
val BackgroundGray = Color(0xFFF5F5F5)

// 메인 화면 전체를 구성하는 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { TopAppBar() },
        bottomBar = { BottomNavigationBar() },
        containerColor = BackgroundGray
    ) { paddingValues ->
        // Column을 스크롤 가능하게 만듭니다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp) // 카드 사이의 간격
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // 상단 여백
            CurrentStatusCard(isSafe = true) // true: 안전, false: 위험
            QuickContactCard()
            DailyCheckInCard(isChecked = true)
            MedicationCard()
            Spacer(modifier = Modifier.height(8.dp)) // 하단 여백
        }
    }
}

// 상단 앱 바
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    "김어르신",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    "2025년 10월 27일 월요일",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

// 1. 현재 상태 카드
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

// 2. 빠른 연락 카드
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

// 빠른 연락 카드에 들어가는 버튼
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

// 3. 오늘의 안부 확인 카드
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

// 4. 오늘의 약 복용 카드
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

// 약 복용 카드에 들어가는 개별 항목
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

// '복용 완료', '확인 완료' 등을 표시하는 작은 태그 (재사용 가능)
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

// 모든 카드의 기본 틀 (재사용 가능)
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

// 하단 네비게이션 바
@Composable
fun BottomNavigationBar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        "홈" to Icons.Default.Home,
        "기록" to Icons.Default.List,
        "설정" to Icons.Default.Settings
    )

    NavigationBar(
        containerColor = Color.White
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}


// Android Studio에서 미리보기를 위한 코드
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GuardianappTheme {
        HomeScreen()
    }
}