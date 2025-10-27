package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

// 약 정보를 담을 데이터 클래스
data class Medication(
    val id: UUID = UUID.randomUUID(), // 고유 ID
    val name: String,
    val time: String,
    var taken: Boolean = false
)
// 화면 경로 정의
object AppDestinations {
    const val HOME = "home"
    const val SAFETY_CHECK = "safety_check"
    const val MEDICATION = "medication"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AnsimTalkApp()
            }
        }
    }
}

@Composable
fun AnsimTalkApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
        modifier = modifier
    ) {
        composable(AppDestinations.HOME) {
            HomeScreen(navController) // NavController 전달
        }
        composable(AppDestinations.SAFETY_CHECK) {
            SafetyCheckScreen(navController)
        }
        composable(AppDestinations.MEDICATION) {
            MedicationScreen(navController)
        }
        composable(AppDestinations.SETTINGS) {
            // 설정 화면 (필요 시 구현)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("설정 화면")
            }
        }
    }
}


@Composable
fun HomeScreen(navController: NavController) { // NavController 파라미터 추가
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header()
        EmergencyCallCard()
        SafetyCheckCard(navController) // NavController 전달
        MedicationCard(navController) // NavController 전달
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCheckScreen(navController: NavController) {
    var selectedMood by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            TopAppBar(
                title = { Text("오늘 안부 확인") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("오늘 기분은 어떠신가요?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("간단히 체크해주시면 보호자님께 알려드립니다.", fontSize = 14.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    MoodOption(
                        text = "좋아요",
                        icon = Icons.Outlined.SentimentVerySatisfied,
                        color = Color(0xFFE8F5E9),
                        isSelected = selectedMood == "좋아요",
                        onClick = { selectedMood = "좋아요" }
                    )
                    MoodOption(
                        text = "보통이에요",
                        icon = Icons.Outlined.SentimentNeutral,
                        color = Color(0xFFFFF9C4),
                        isSelected = selectedMood == "보통이에요",
                        onClick = { selectedMood = "보통이에요" }
                    )
                    MoodOption(
                        text = "안 좋아요",
                        icon = Icons.Outlined.SentimentDissatisfied,
                        color = Color(0xFFFFEBEE),
                        isSelected = selectedMood == "안 좋아요",
                        onClick = { selectedMood = "안 좋아요" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* 확인 완료 로직 */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMood != null) MaterialTheme.colorScheme.primary else Color.Gray,
                            contentColor = Color.White
                        ),
                        enabled = selectedMood != null
                    ) {
                        Text("확인 완료", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MoodOption(text: String, icon: ImageVector, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(navController: NavController) {
    // 1. 약 목록 상태 관리
    val medicationList = remember {
        mutableStateListOf(
            Medication(name = "철분약", time = "08:00", taken = true),
            Medication(name = "당뇨약", time = "08:00", taken = true),
            Medication(name = "소화제", time = "12:00", taken = true),
            Medication(name = "혈압약", time = "18:00", taken = false),
            Medication(name = "당뇨약", time = "18:00", taken = false)
        )
    }

    // 2. 다이얼로그 표시 여부 상태
    var showDialog by remember { mutableStateOf(false) }

    // ★ 추가: 복용 현황 계산
    val totalPills = medicationList.size
    val takenPills = medicationList.count { it.taken }

    // 3. 약 추가 다이얼로그
    if (showDialog) {
        AddMedicationDialog(
            onDismiss = { showDialog = false },
            onAdd = { name, time ->
                medicationList.add(Medication(name = name, time = time))
                showDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 바
        item {
            TopAppBar(
                title = { Text("약 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    // '추가' 버튼 클릭 시 다이얼로그를 띄움
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "추가")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("추가")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        // 오늘의 복용 현황
        item {
            // ★ 수정: 계산된 값을 파라미터로 전달
            MedicationStatusCard(takenCount = takenPills, totalCount = totalPills)
        }

        // 복용 일정
        item {
            Text("복용 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 4. 상태 리스트를 기반으로 아이템 렌더링
                medicationList.forEach { medication ->
                    MedicationScheduleItem(
                        medication = medication,
                        onTakePill = {
                            // '복용' 버튼 클릭 시 taken 상태 변경
                            val index = medicationList.indexOf(medication)
                            if (index != -1) {
                                // 상태를 업데이트하여 리컴포지션 유발
                                medicationList[index] = medication.copy(taken = true)
                            }
                        }
                    )
                }
            }
        }

        // 이번 주 복용 기록
        item {
            Text("이번 주 복용 기록", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WeeklyLogItem(date = "2025-10-10", progress = "3/5")
                    WeeklyLogItem(date = "2025-10-09", progress = "5/5", completed = true)
                    WeeklyLogItem(date = "2025-10-08", progress = "5/5", completed = true)
                    WeeklyLogItem(date = "2025-10-07", progress = "4/5")
                    WeeklyLogItem(date = "2025-10-06", progress = "5/5", completed = true)
                }
            }
        }
    }
}


// 약 추가 다이얼로그
@Composable
fun AddMedicationDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var medName by remember { mutableStateOf("") }
    var medTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("약 추가하기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("약 이름") }
                )
                OutlinedTextField(
                    value = medTime,
                    onValueChange = { medTime = it },
                    label = { Text("복용 시간 (예: 09:00)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (medName.isNotBlank() && medTime.isNotBlank()) {
                        onAdd(medName, medTime)
                    }
                }
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun MedicationStatusCard(takenCount: Int, totalCount: Int) { // ★ 파라미터 추가
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = "약 아이콘", tint = Color(0xFF9575CD))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("오늘의 복용 현황", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // ★ 수정: 파라미터를 사용하여 동적으로 텍스트 표시
                Text("$takenCount/$totalCount", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                // ★ 추가: 모든 약을 복용했는지에 따라 다른 텍스트 표시
                if (totalCount > 0 && takenCount == totalCount) {
                    Text("모두 복용 완료!", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("복용 진행 중", color = Color.Gray)
                }
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "완료",
                    // ★ 추가: 모든 약을 복용했을 때만 아이콘을 진하게 표시
                    tint = if (totalCount > 0 && takenCount == totalCount) Color(0xFF4CAF50) else Color.LightGray,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}


// MedicationScheduleItem 함수 수정
@Composable
fun MedicationScheduleItem(medication: Medication, onTakePill: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "약",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (medication.taken) Color(0xFFE8F5E9) else Color(0xFFE0E0E0))
                        .padding(8.dp),
                    tint = if (medication.taken) Color(0xFF4CAF50) else Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(medication.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = "시간", modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(medication.time, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            // '복용 완료' 상태일 때와 아닐 때를 구분하여 표시
            if (medication.taken) {
                Text(
                    text = "복용 완료",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Button(
                    onClick = onTakePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("복용")
                }
            }
        }
    }
}

@Composable
fun WeeklyLogItem(date: String, progress: String, completed: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(date, color = Color.Gray, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(progress, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (completed) {
                Text(
                    "완료",
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- 홈화면 전용 컴포넌트들 ---
@Composable
fun Header() {
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREAN)
    val currentDate = dateFormat.format(Date())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("안심톡", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(currentDate, fontSize = 16.sp, color = Color.Gray)
    }
}

@Composable
fun EmergencyCallCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 긴급 호출 로직 */ }, // 클릭 가능하게 변경
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "긴급 호출 아이콘",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "긴급 호출",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SafetyCheckCard(navController: NavController) { // NavController 파라미터 추가
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(AppDestinations.SAFETY_CHECK) }, // 클릭 시 화면 이동
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "안부 확인 아이콘",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("오늘의 안부 확인", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("마지막 확인: 오전 9:30", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "확인 완료",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = { navController.navigate(AppDestinations.SAFETY_CHECK) }, // 버튼 클릭 시 화면 이동
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("지금 확인하기", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun MedicationCard(navController: NavController) { // NavController 파라미터 추가
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(AppDestinations.MEDICATION) }, // 클릭 시 화면 이동
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "약 복용 아이콘",
                    tint = Color(0xFF9575CD),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("오늘의 약 복용", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            MedicationItem("아침 약", "오전 8:00", true)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            MedicationItem("점심 약", "오후 12:00", true)
        }
    }
}

@Composable
fun MedicationItem(medName: String, time: String, taken: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(medName, fontSize = 16.sp)
            Text(time, fontSize = 14.sp, color = Color.Gray)
        }
        if (taken) {
            Text(
                text = "복용 완료",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem("홈", Icons.Filled.Home, AppDestinations.HOME),
        BottomNavItem("안부확인", Icons.Filled.Check, AppDestinations.SAFETY_CHECK),
        BottomNavItem("약", Icons.Outlined.Info, AppDestinations.MEDICATION),
        BottomNavItem("설정", Icons.Filled.Settings, AppDestinations.SETTINGS)
    )

    NavigationBar(
        containerColor = Color.White,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

// route 프로퍼티 추가
data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)

// --- Preview 모음 ---

@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomePreview() {
    MyApplicationTheme {
        HomeScreen(rememberNavController())
    }
}

@Preview(showBackground = true, name = "Safety Check Screen Preview")
@Composable
fun SafetyCheckPreview() {
    MyApplicationTheme {
        SafetyCheckScreen(rememberNavController())
    }
}

@Preview(showBackground = true, name = "Medication Screen Preview")
@Composable
fun MedicationPreview() {
    MyApplicationTheme {
        MedicationScreen(rememberNavController())
    }
}
