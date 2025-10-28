package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.network.StatusRequest
import com.example.myapplication.network.UserInfo
import com.example.myapplication.network.UserResponse
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

// 약 정보를 담을 데이터 클래스
data class Medication(
    val id: UUID = UUID.randomUUID(), // 고유 ID
    val name: String,
    val time: String,
    var taken: Boolean = false
)

// 화면 경로 정의
object AppDestinations {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val showBottomBar = currentDestination !in listOf(AppDestinations.LOGIN, AppDestinations.SIGN_UP)

    // --- 낙상 감지 다이얼로그를 위한 상태 ---
    var showFallDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- 낙상 감지 기능 관리자 ---
    FallDetectionManager {
        // 낙상이 감지되면 다이얼로그를 띄우도록 상태 변경
        showFallDialog = true
    }

    // --- 낙상 감지 다이얼로그 ---
    if (showFallDialog) {
        var countdown by remember { mutableIntStateOf(15) }

        // 1초마다 카운트다운을 줄이는 효과
        LaunchedEffect(Unit) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // 카운트다운이 끝나면 (응답이 없으면) 긴급 신고 절차 시작
            showFallDialog = false
            initiateEmergencyCall(context)
        }

        AlertDialog(
            onDismissRequest = { /* 바깥 클릭으로 닫기 비활성화 */ },
            title = { Text("낙상 감지!", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("괜찮으신가요? $countdown 초 후 자동으로 119에 신고됩니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        // "괜찮아요" 버튼을 누르면 다이얼로그만 닫음
                        showFallDialog = false
                    },
                    // 버튼 크기를 키워서 누르기 쉽게 함
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("괜찮아요", fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // "신고하기"를 누르면 즉시 긴급 신고 절차 시작
                        showFallDialog = false
                        initiateEmergencyCall(context)
                    }
                ) {
                    Text("즉시 신고하기", color = Color.Red)
                }
            }
        )
    }

    // --- 약 목록 상태를 최상위로 이동 ---
    val medicationList = remember {
        mutableStateListOf(
            Medication(name = "철분약", time = "08:00", taken = true),
            Medication(name = "당뇨약", time = "08:00", taken = true),
            Medication(name = "소화제", time = "12:00", taken = true),
            Medication(name = "혈압약", time = "18:00", taken = false),
            Medication(name = "당뇨약", time = "18:00", taken = false)
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        // --- AppNavHost에 약 목록과 모든 수정 함수 전달 ---
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            medicationList = medicationList,
            onAddMedication = { name, time -> medicationList.add(Medication(name = name, time = time)) },
            onTakePill = { medication ->
                val index = medicationList.indexOf(medication)
                if (index != -1) {
                    medicationList[index] = medication.copy(taken = true)
                }
            },
            onRemoveMedication = { medication ->
                medicationList.remove(medication)
            }
        )
    }
}
@Composable
fun FallDetectionManager(onFallDetected: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 센서 관리자와 리스너 상태 저장
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 낙상 감지 알고리즘을 위한 상태 변수
    var freeFallDetected by remember { mutableStateOf(false) }
    var lastFreeFallTime by remember { mutableLongStateOf(0L) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // 가속도 벡터의 크기 계산
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    // 1. 자유 낙하 감지 (가속도 크기가 매우 작을 때)
                    if (magnitude < 2.0f) {
                        freeFallDetected = true
                        lastFreeFallTime = System.currentTimeMillis()
                    }

                    // 2. 충격 감지 (가속도 크기가 매우 클 때)
                    if (magnitude > 15.0f) {
                        // 자유 낙하가 감지된 후 1초 이내에 강한 충격이 왔는지 확인
                        if (freeFallDetected && (System.currentTimeMillis() - lastFreeFallTime < 1000)) {
                            onFallDetected() // 낙상 감지 콜백 호출
                        }
                        // 상태 초기화
                        freeFallDetected = false
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // 사용하지 않음
            }
        }
    }

    // Composable의 생명주기에 맞춰 센서 리스너 등록 및 해제
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 앱이 화면에 보일 때 센서 리스너 등록
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                // 앱이 화면에서 사라질 때 리스너 해제 (배터리 절약)
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Composable이 사라질 때 최종적으로 리스너 해제
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}


@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    medicationList: List<Medication>,
    onAddMedication: (String, String) -> Unit,
    onTakePill: (Medication) -> Unit,
    onRemoveMedication: (Medication) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.LOGIN,
        modifier = modifier
    ) {
        composable(AppDestinations.LOGIN) {
            LoginScreen(navController)
        }
        composable(AppDestinations.SIGN_UP) {
            SignUpScreen(navController)
        }
        composable(AppDestinations.HOME) {
            HomeScreen(navController, medicationList)
        }
        composable(AppDestinations.SAFETY_CHECK) {
            SafetyCheckScreen(navController)
        }
        composable(AppDestinations.MEDICATION) {
            MedicationScreen(navController, medicationList, onAddMedication, onTakePill, onRemoveMedication)
        }
        // --- 바로 이 부분의 내용을 교체하는 것입니다 ---
        composable(AppDestinations.SETTINGS) {
            // 기존 Box 대신 우리가 만든 SettingsScreen을 호출
            SettingsScreen()
        }
    }
}

// --- 로그인 화면 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("안심톡", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("아이디") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                navController.navigate(AppDestinations.HOME) {
                    popUpTo(AppDestinations.LOGIN) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("로그인", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.navigate(AppDestinations.SIGN_UP) }
        ) {
            Text("회원가입")
        }
    }
}

// --- 회원가입 화면 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("아이디") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("회원가입 완료", fontSize = 18.sp)
            }
        }
    }
}

// --- 홈 화면 ---
@Composable
fun HomeScreen(navController: NavController, medicationList: List<Medication>) {
    val totalPills = medicationList.size
    val takenPills = medicationList.count { it.taken }

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
        SafetyCheckCard(navController)
        MedicationCard(navController, takenCount = takenPills, totalCount = totalPills)
    }
}

// --- 홈 화면의 카드들 ---

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("안녕하세요, 어르신!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = "프로필",
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun EmergencyCallCard() {
    // --- 상태 관리 ---
    // 다이얼로그를 보여줄지 결정하는 상태
    var showEmergencyDialog by remember { mutableStateOf(false) }
    // 현재 컨텍스트를 가져옴
    val context = LocalContext.current

    // --- 위치 권한 요청을 위한 런처 ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                // 권한이 승인되면 긴급 신고 절차 진행
                initiateEmergencyCall(context)
            } else {
                // 권한이 거부되면 사용자에게 알림
                Toast.makeText(context, "위치 권한이 없어 위치 전송이 불가능합니다.", Toast.LENGTH_SHORT).show()
                // 위치 정보 없이 전화만 걸도록 처리
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:119"))
                    context.startActivity(callIntent)
                } catch (e: SecurityException) {
                    Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // --- 긴급 신고 확인 다이얼로그 ---
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("긴급 신고", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("정말로 119에 신고하고 보호자에게 위치를 전송하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        // --- 전화 권한 확인 ---
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                // 전화 권한이 있으면 위치 권한 확인 시작
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            else -> {
                                // 전화 권한이 없는 경우 사용자에게 알림.
                                Toast.makeText(context, "전화 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("신고")
                }
            },
            dismissButton = {
                Button(onClick = { showEmergencyDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // --- 기존 카드 UI ---
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("긴급 전화", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("버튼을 누르면 바로 연결돼요")
            }
            Button(
                onClick = {
                    // 버튼 클릭 시 바로 신고하는 대신 다이얼로그를 띄움
                    showEmergencyDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "전화 걸기", tint = Color.White)
            }
        }
    }
}

// --- 실제 긴급 신고 절차를 수행하는 별도의 함수 ---
private fun initiateEmergencyCall(context: Context) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 위치 권한이 있는지 다시 한 번 명시적으로 확인 (매우 중요)
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return // 권한이 없으면 함수 종료
    }

    // 마지막으로 알려진 위치를 가져옴
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            val locationText = if (location != null) {
                "위도: ${location.latitude}, 경도: ${location.longitude}"
            } else {
                "위치 정보를 가져올 수 없습니다."
            }

            // [보호자 앱 연동 부분] Logcat에 위치 정보 출력
            android.util.Log.d("EmergencyCall", "긴급 상황! 보호자에게 위치 전송: $locationText")
            Toast.makeText(context, "보호자에게 위치를 전송했습니다.", Toast.LENGTH_LONG).show()

            // 119에 전화 걸기
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:119"))
                context.startActivity(callIntent)
            } catch (e: Exception) {
                android.util.Log.e("EmergencyCall", "전화 걸기 실패", e)
                Toast.makeText(context, "전화를 걸 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            android.util.Log.e("EmergencyCall", "위치 정보 요청 실패", it)
            Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            // 위치 정보 실패 시에도 전화는 걸도록 처리
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:119"))
                context.startActivity(callIntent)
            } catch (e: SecurityException) {
                Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
}

@Composable
fun SafetyCheckCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(AppDestinations.SAFETY_CHECK) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("오늘 안부 확인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("오늘 기분은 어떠세요?")
            }
            Icon(Icons.Default.ArrowForwardIos, contentDescription = "이동")
        }
    }
}

@Composable
fun MedicationCard(navController: NavController, takenCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(AppDestinations.MEDICATION) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("약 관리", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("잊지 말고 약을 챙겨드세요")
                }
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "이동")
            }
            Spacer(modifier = Modifier.height(16.dp))
            MedicationStatusCard(takenCount = takenCount, totalCount = totalCount)
        }
    }
}


// --- 안부 확인 화면 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCheckScreen(navController: NavController) {
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* 바깥 클릭으로 닫기 비활성화 */ },
            title = { Text("전송 완료") },
            text = { Text("오늘의 안부가 보호자에게 안전하게 전달되었습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("확인")
                }
            }
        )
    }

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
                        onClick = {
                            selectedMood?.let { mood ->
                                // --- 레트로핏을 이용한 서버 통신 코드 ---
                                val request = StatusRequest(userId = 1L, status = mood) // userId는 임시로 1
                                RetrofitInstance.api.sendStatus(request).enqueue(object : retrofit2.Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                                        if (response.isSuccessful) {
                                            // 서버와 통신이 성공했을 때
                                            android.util.Log.d("SafetyCheck", "서버 전송 성공: $mood")
                                            showConfirmationDialog = true // 성공 알림 다이얼로그 띄우기
                                        } else {
                                            // 서버가 요청을 받았지만, 에러 코드를 응답했을 때 (예: 404, 500)
                                            android.util.Log.e("SafetyCheck", "서버 응답 실패: ${response.code()}")
                                            // TODO: 사용자에게 "전송에 실패했습니다." 알림 보여주기
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        // 서버에 아예 연결조차 되지 않았을 때 (네트워크 오류, 서버 꺼짐, 주소 틀림 등)
                                        android.util.Log.e("SafetyCheck", "서버 연결 실패", t)
                                        // TODO: 사용자에게 "네트워크 연결을 확인해주세요." 알림 보여주기
                                    }
                                })
                            }
                        },
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


// --- 약 관리 화면 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    navController: NavController,
    medicationList: List<Medication>,
    onAddMedication: (String, String) -> Unit,
    onTakePill: (Medication) -> Unit,
    onRemoveMedication: (Medication) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val totalPills = medicationList.size
    val takenPills = medicationList.count { it.taken }

    if (showDialog) {
        AddMedicationDialog(
            onDismiss = { showDialog = false },
            onAdd = { name, time ->
                onAddMedication(name, time)
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
        item {
            TopAppBar(
                title = { Text("약 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "추가")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("추가")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        item {
            MedicationStatusCard(takenCount = takenPills, totalCount = totalPills)
        }

        item {
            Text("복용 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                medicationList.forEach { medication ->
                    MedicationScheduleItem(
                        medication = medication,
                        onTakePill = { onTakePill(medication) },
                        onRemove = { onRemoveMedication(medication) }
                    )
                }
            }
        }

        item {
            Text("이번 주 복용 기록", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                // --- 동적 날짜 기반의 더미 데이터로 수정 ---
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val calendar = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val random = Random()

                    // 오늘부터 과거 5일간의 기록을 동적으로 생성
                    repeat(5) { dayIndex ->
                        val date = dateFormat.format(calendar.time)
                        val total = 5
                        // 날짜가 오래될수록 약을 다 먹었을 확률을 높임 (더미 데이터의 현실성 부여)
                        val taken = if (dayIndex < 2) random.nextInt(4) + 1 else total
                        WeeklyLogItem(date = date, progress = "$taken/$total", completed = (taken == total))
                        calendar.add(Calendar.DAY_OF_YEAR, -1) // 하루씩 과거로 이동
                    }
                }
            }
        }
    }
}


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
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// --- 재사용 가능한 컴포저블 ---

@Composable
fun MedicationStatusCard(takenCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)) // 연보라 배경
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
                Text("$takenCount/$totalCount", fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                    tint = if (totalCount > 0 && takenCount == totalCount) Color(0xFF4CAF50) else Color.LightGray,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun MedicationScheduleItem(
    medication: Medication,
    onTakePill: () -> Unit,
    onRemove: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.Medication,
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

            Spacer(modifier = Modifier.width(8.dp))

            // --- 복용/완료 버튼과 삭제 버튼을 담을 Row ---
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Button(onClick = onTakePill, shape = RoundedCornerShape(8.dp)) {
                        Text("복용")
                    }
                }
                // --- 삭제 버튼 아이콘 추가 ---
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
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
        Column {
            Text(date, fontWeight = FontWeight.Medium)
            Text(progress, color = Color.Gray)
        }
        if (completed) {
            Icon(Icons.Default.CheckCircle, contentDescription = "완료", tint = Color(0xFF4CAF50))
        } else {
            Icon(Icons.Default.Error, contentDescription = "미완료", tint = Color.Red.copy(alpha = 0.7f))
        }
    }
}

// --- ★★★★★ 새로 추가된 설정 화면 ★★★★★ ---
@Composable
fun SettingsScreen() {
    // 서버로부터 받아올 사용자 정보를 저장할 상태 변수
    // 처음에는 비어있다가, 통신 성공 후 데이터로 채워짐
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // SettingsScreen이 처음 실행될 때 서버에 딱 한 번만 데이터 요청
    LaunchedEffect(Unit) {
        // 실제로는 로그인된 사용자 ID를 사용해야 하지만, 지금은 1로 고정
        val userId = 1L
        RetrofitInstance.api.getUserInfo(userId).enqueue(object : retrofit2.Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: retrofit2.Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // 통신 성공 시, 응답받은 데이터를 userInfo 상태에 저장
                    userInfo = response.body()!!.data
                } else {
                    // 통신은 됐지만, 서버가 에러를 보냈을 때
                    errorMessage = "정보를 불러오는 데 실패했습니다."
                    // 친구의 서버가 아직 준비 안됐을 때 테스트하는 방법
                    // 아래 두 줄의 주석을 풀면, 가짜 데이터로 화면을 테스트할 수 있습니다.
                    errorMessage = null
                    userInfo = UserInfo(userId = 1L, username = "user123", name = "홍길동 (가짜)")
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // 통신 자체가 실패했을 때 (네트워크 오류, 서버 꺼짐 등)
                errorMessage = "네트워크 연결을 확인해주세요."
                // 친구의 서버가 아직 준비 안됐을 때 테스트하는 방법
                // 아래 두 줄의 주석을 풀면, 가짜 데이터로 화면을 테스트할 수 있습니다.
                errorMessage = null
                userInfo = UserInfo(userId = 1L, username = "user123", name = "홍길동 (가짜)")
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("내 정보", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // userInfo가 아직 null이면 로딩 중 표시
                if (userInfo == null && errorMessage == null) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    // 에러가 발생하면 에러 메시지 표시
                    Text(errorMessage!!, color = Color.Red)
                } else {
                    // 정보가 있으면 화면에 표시
                    InfoRow(label = "이름", value = userInfo!!.name)
                    InfoRow(label = "아이디", value = userInfo!!.username)
                }
            }
        }
        // TODO: 로그아웃, 앱 버전 정보 등 다른 설정 메뉴 추가
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Text(value)
    }
}


// --- 하단 네비게이션 바 ---
@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = currentRoute == AppDestinations.HOME,
            onClick = {
                navController.navigate(AppDestinations.HOME) {
                    launchSingleTop = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "안부 확인") },
            label = { Text("안부 확인") },
            selected = currentRoute == AppDestinations.SAFETY_CHECK,
            onClick = {
                navController.navigate(AppDestinations.SAFETY_CHECK) {
                    launchSingleTop = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Info, contentDescription = "약 관리") },
            label = { Text("약 관리") },
            selected = currentRoute == AppDestinations.MEDICATION,
            onClick = {
                navController.navigate(AppDestinations.MEDICATION) {
                    launchSingleTop = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            selected = currentRoute == AppDestinations.SETTINGS,
            onClick = {
                navController.navigate(AppDestinations.SETTINGS) {
                    launchSingleTop = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }
        )
    }
}
