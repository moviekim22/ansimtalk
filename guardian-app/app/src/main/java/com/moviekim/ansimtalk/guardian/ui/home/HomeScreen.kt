package com.moviekim.ansimtalk.guardian.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.moviekim.ansimtalk.guardian.ui.api.RetrofitClient
import com.moviekim.ansimtalk.guardian.ui.dto.ConnectionResponse
import com.moviekim.ansimtalk.guardian.ui.dto.LogResponseDto
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme
import com.moviekim.ansimtalk.guardian.ui.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

val StatusGreen = Color(0xFF4CAF50)
val StatusRed = Color(0xFFD32F2F)
val StatusGray = Color(0xFFBDBDBD)
val BackgroundGray = Color(0xFFF5F5F5)

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userName = sessionManager.getUserName() ?: "보호자"

    // 연결된 어르신 정보
    var connectedElderly by remember { mutableStateOf<ConnectionResponse?>(null) }
    // 약 복용 기록
    var medicationLogs by remember { mutableStateOf<List<LogResponseDto>>(emptyList()) }
    // 데이터 로딩 상태
    var isLoading by remember { mutableStateOf(true) }

    // 화면이 처음 그려질 때 데이터 로드
    LaunchedEffect(Unit) {
        // 1. 연결된 어르신 정보 가져오기
        RetrofitClient.apiService.getMyConnections().enqueue(object : Callback<List<ConnectionResponse>> {
            override fun onResponse(call: Call<List<ConnectionResponse>>, response: Response<List<ConnectionResponse>>) {
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val elderly = response.body()!!.first() // 첫 번째 연결된 어르신 정보 사용
                    connectedElderly = elderly

                    // 2. 어르신 복용 기록 가져오기 (최근 7일)
                    RetrofitClient.apiService.getMedicationLogs(elderly.elderlyId, 7).enqueue(object : Callback<List<LogResponseDto>> {
                        override fun onResponse(call: Call<List<LogResponseDto>>, response: Response<List<LogResponseDto>>) {
                            if (response.isSuccessful) {
                                medicationLogs = response.body() ?: emptyList()
                            }
                            isLoading = false // 데이터 로드 완료
                        }

                        override fun onFailure(call: Call<List<LogResponseDto>>, t: Throwable) {
                            Toast.makeText(context, "복용 기록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    })
                } else {
                    isLoading = false // 연결된 어르신 없음, 로딩 종료
                }
            }

            override fun onFailure(call: Call<List<ConnectionResponse>>, t: Throwable) {
                Toast.makeText(context, "연결 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 16.dp)
    ) {
        TopAppBar(userName = userName, elderlyName = connectedElderly?.elderlyName)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            // 로딩 중 표시
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { CurrentStatusCard(isSafe = true) } // 이 부분은 실제 데이터로 추후 변경 필요
                item { QuickContactCard() }
                item { DailyCheckInCard(isChecked = true) } // 이 부분도 실제 데이터로 추후 변경 필요

                item {
                    // 어르신 연결 상태에 따라 다른 화면 표시
                    if (connectedElderly == null) {
                        NoConnectionCard(navController)
                    } else {
                        MedicationCard(medicationLogs)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) } // 하단 여백
            }
        }
    }
}

// 어르신과 연결되지 않았을 때 표시되는 카드
@Composable
fun NoConnectionCard(navController: NavController) {
    InfoCard(icon = Icons.Default.Warning, title = "어르신 연결 필요") {
        Text("어르신과 연결하고 복용 기록을 확인하세요.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("설정으로 이동")
        }
    }
}


@Composable
fun TopAppBar(userName: String, elderlyName: String?) {
    val titleText = elderlyName?.let { "$it 님 (보호자: $userName 님)" } ?: "${userName}님"
    val currentDate = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.getDefault()).format(Date())

    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Text(
            text = titleText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = currentDate,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
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
fun MedicationCard(logs: List<LogResponseDto>) {
    InfoCard(icon = Icons.Default.Info, title = "최근 복용 기록") {
        if (logs.isEmpty()) {
            Text("최근 7일간의 복용 기록이 없습니다.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        } else {
            // 날짜별로 그룹화
            val groupedLogs = logs.groupBy { it.date }.toSortedMap(compareByDescending { it })
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groupedLogs.forEach { (date, logsOnDate) ->
                    DateHeader(date)
                    logsOnDate.forEach {
                        MedicationItem(log = it)
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Text(
        text = date,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}


@Composable
fun MedicationItem(log: LogResponseDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(log.medicationName, fontWeight = FontWeight.Bold)
            Text("예정: ${log.scheduleTime}", color = Color.Gray, fontSize = 12.sp)
            if (log.takenAt != null) {
                Text("복용: ${log.takenAt}", color = StatusGreen, fontSize = 12.sp)
            }
        }
        StatusChip(
            text = if (log.isTaken) "복용 완료" else "복용 전",
            isCompleted = log.isTaken
        )
    }
}

@Composable
fun StatusChip(text: String, isCompleted: Boolean) {
    val backgroundColor = if (isCompleted) StatusGreen else StatusRed
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
        // Preview에서는 NavController를 직접 생성하여 전달합니다.
        HomeScreen(navController = NavController(LocalContext.current))
    }
}
