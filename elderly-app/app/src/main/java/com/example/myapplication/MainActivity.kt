
package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.dto.EventRequest
import com.example.myapplication.dto.EventType
import com.example.myapplication.dto.LoginResponse
import com.example.myapplication.dto.UserInfo
import com.example.myapplication.dto.UserResponse
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.UserSessionManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

object GuardianContactManager {
    private const val PREFS_NAME = "ansimtalk_prefs"
    private const val KEY_GUARDIAN_PHONE = "guardian_phone"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveGuardianPhone(context: Context, phoneNumber: String) {
        getPreferences(context).edit().putString(KEY_GUARDIAN_PHONE, phoneNumber).apply()
    }

    fun getGuardianPhone(context: Context): String? {
        return getPreferences(context).getString(KEY_GUARDIAN_PHONE, null)
    }
}

data class Medication(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val time: String,
    var taken: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSessionManager.init(this)

        setContent {
            MyApplicationTheme {
                AnsimTalkApp(intent = intent)
            }
        }
    }

    // 앱이 이미 실행 중일 때 새로운 인텐트(낙상 감지)를 처리하기 위해 필요
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로 들어온 인텐트로 교체
        // UI를 다시 그리도록 setContent를 다시 호출
        setContent {
            MyApplicationTheme {
                AnsimTalkApp(intent = intent)
            }
        }
    }
}

@Composable
fun AnsimTalkApp(intent: Intent) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val isFallDetected = intent.getBooleanExtra("FALL_DETECTED", false)
    val isInactivityDetected = intent.getBooleanExtra("INACTIVITY_DETECTED", false)

    val isLoggedIn = UserSessionManager.loadSession()
    val startDestination = if (isFallDetected || isInactivityDetected || isLoggedIn) AppDestinations.HOME else AppDestinations.LOGIN

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.all { it.value }) {
                ContextCompat.startForegroundService(context, Intent(context, DetectionService::class.java))
            } else {
                Toast.makeText(context, "서비스를 위해 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(Manifest.permission.FOREGROUND_SERVICE)
        }
        val allGranted = permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            ContextCompat.startForegroundService(context, Intent(context, DetectionService::class.java))
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // 테스트용 약물 목록 데이터 복원
    val medicationList = remember {
        mutableStateListOf(
            Medication(name = "철분약", time = "08:00", taken = true),
            Medication(name = "당뇨약", time = "12:00", taken = false),
        )
    }

    // 4. UI 구성
    Scaffold(
        bottomBar = {
            val showBottomBar = currentDestination !in listOf(AppDestinations.LOGIN, AppDestinations.SIGN_UP)
            if (showBottomBar) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = startDestination,
            isFallDetected = isFallDetected, // 낙상 감지 상태 전달
            isInactivityDetected = isInactivityDetected,
            medicationList = medicationList,
            onAddMedication = { name, time -> medicationList.add(Medication(name = name, time = time)) },
            onTakePill = { medication ->
                val index = medicationList.indexOf(medication)
                if (index != -1) medicationList[index] = medication.copy(taken = true)
            },
            onRemoveMedication = { medication ->
                medicationList.remove(medication)
            }
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String,
    isFallDetected: Boolean, // 낙상 감지 상태 수신
    isInactivityDetected: Boolean,
    medicationList: List<Medication>,
    onAddMedication: (String, String) -> Unit,
    onTakePill: (Medication) -> Unit,
    onRemoveMedication: (Medication) -> Unit
) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    composable(AppDestinations.LOGIN) {
            LoginScreen(navController = navController, onLoginSuccess = { user ->
                UserSessionManager.login(context, user)
                navController.navigate(AppDestinations.HOME) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
            })
        }
        composable(AppDestinations.SIGN_UP) { SignUpScreen(navController) }
        composable(AppDestinations.HOME) {
            // 낙상이 감지되었지만 로그인이 안된 경우를 대비해, 사용자 이름을 기본값으로 설정
            val userName = UserSessionManager.currentUser?.name ?: "사용자"
            HomeScreen(
                navController = navController,
                userName = userName,
                medicationList = medicationList,
                isFallDetected = isFallDetected, // 낙상 감지 상태를 홈 화면에 전달
                isInactivityDetected = isInactivityDetected
            )
        }
        composable(AppDestinations.MEDICATION) {
            MedicationScreen(navController, medicationList, onAddMedication, onTakePill, onRemoveMedication)
        }
        composable(AppDestinations.SETTINGS) { SettingsScreen(navController = navController) }
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    userName: String,
    medicationList: List<Medication>,
    isFallDetected: Boolean, // 낙상 감지 상태 수신
    isInactivityDetected: Boolean

) {
    var showFallDialog by remember { mutableStateOf(isFallDetected) }
    var showInactivityDialog by remember { mutableStateOf(isInactivityDetected) }

    // 낙상 감지 다이얼로그 표시
    if (showFallDialog) {
        FallDetectionDialog(onDismiss = { showFallDialog = false })
    }
    if (showInactivityDialog) {
        InactivityDialog(onDismiss = { showInactivityDialog = false })
    }
    val totalPills = medicationList.size
    val takenPills = medicationList.count { it.taken }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(userName = userName)
        EmergencyCallCard()
        MedicationCard(navController, takenCount = takenPills, totalCount = totalPills)
    }
}

// 낙상 감지 다이얼로그 Composable
@Composable
fun FallDetectionDialog(onDismiss: () -> Unit) {
    var countdown by remember { mutableStateOf(30) }
    val context = LocalContext.current

    // 30초 카운트다운 로직
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        if (countdown == 0) {
            // 카운트다운이 끝나면 보호자에게 알림 전송
            Toast.makeText(context, "보호자에게 메시지를 전송합니다.", Toast.LENGTH_LONG).show()
            sendFallReportToGuardian(context) // 이벤트 전송 함수 호출
            onDismiss() // 다이얼로그 닫기
        }
    }

    AlertDialog(
        onDismissRequest = { /* 사용자가 외부를 클릭해도 꺼지지 않도록 비워둠 */ },
        title = {
            Text("낙상 감지!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text("괜찮으신가요? ${countdown}초 후 보호자에게 자동으로 메시지가 전송됩니다.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = onDismiss, // '괜찮아요'를 누르면 다이얼로그만 닫힘
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("괜찮아요", fontSize = 18.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    Toast.makeText(context, "즉시 보호자에게 메시지를 전송합니다.", Toast.LENGTH_LONG).show()
                    sendFallReportToGuardian(context) // 즉시 전송
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("즉시 전송하기", color = Color.Gray)
            }
        }
    )
}
// --- 장시간 미사용 다이얼로그 추가 ---
@Composable
fun InactivityDialog(onDismiss: () -> Unit) {
    var countdown by remember { mutableStateOf(60) } // 60초 카운트다운
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        if (countdown == 0) {
            Toast.makeText(context, "보호자에게 응답이 없음을 알립니다.", Toast.LENGTH_LONG).show()
            // TODO: 미응답 이벤트 서버 전송 로직 추가
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("괜찮으세요?", fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text("장시간 활동이 감지되지 않았습니다. ${countdown}초 후 보호자에게 알림이 전송됩니다.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("네, 괜찮아요", fontSize = 18.sp)
            }
        }
    )
}

// 낙상 감지 이벤트를 서버로 전송하는 함수
private fun sendFallReportToGuardian(context: Context) {
    // 위치 권한이 없으면 전송 불가
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "위치 권한이 없어 전송할 수 없습니다.", Toast.LENGTH_LONG).show()
        return
    }

    // 로그인 상태가 아니어도 전송을 시도하되, ID가 있으면 포함시킴
    val userId = UserSessionManager.currentUser?.id ?: -1L // 비로그인 시 -1과 같은 임시 ID 사용

    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location: Location? ->
        val eventRequest = EventRequest(userId, EventType.FALL_DETECTED, location?.latitude ?: 0.0, location?.longitude ?: 0.0)
        RetrofitInstance.api.sendEmergencyEvent(eventRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, "보호자에게 알림을 보내는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "네트워크 오류로 보호자에게 알림을 보낼 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }.addOnFailureListener {
        Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun Header(userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("안녕하세요, $userName 님!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.AccountCircle, contentDescription = "프로필", modifier = Modifier.size(40.dp))
    }
}

@Composable
fun EmergencyCallCard() {
    var showEmergencyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.values.all { it }) {
                initiateEmergencyCall(context)
            } else {
                Toast.makeText(context, "위치 권한이 거부되었습니다.", Toast.LENGTH_LONG).show()
            }
        }
    )

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("긴급 호출", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("정말로 긴급 호출을 하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("호출") }
            },
            dismissButton = { Button(onClick = { showEmergencyDialog = false }) { Text("취소") } }
        )
    }

    Button(
        onClick = { showEmergencyDialog = true },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Warning, contentDescription = "긴급 호출 아이콘", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("긴급 호출", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun initiateEmergencyCall(context: Context) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "위치 권한이 없어 호출을 할 수 없습니다.", Toast.LENGTH_LONG).show()
        return
    }
    val userId = UserSessionManager.currentUser?.id
    if (userId == null) {
        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        return
    }
    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location: Location? ->
        val eventRequest = EventRequest(userId, EventType.EMERGENCY_CALL, location?.latitude ?: 0.0, location?.longitude ?: 0.0)
        RetrofitInstance.api.sendEmergencyEvent(eventRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) Toast.makeText(context, "긴급 호출이 성공적으로 전송되었습니다.", Toast.LENGTH_LONG).show()
                else Toast.makeText(context, "긴급 호출 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "네트워크 오류로 긴급 호출에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }.addOnFailureListener { e ->
        Log.e("EmergencyCall", "위치 정보 가져오기 실패", e)
        Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun MedicationCard(navController: NavController, takenCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(AppDestinations.MEDICATION) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
            onAdd = { name, time -> onAddMedication(name, time); showDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TopAppBar(
                title = { Text("약 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") } },
                actions = { Button(onClick = { showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "추가"); Text("추가") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
        item { MedicationStatusCard(takenCount = takenPills, totalCount = totalPills) }
        item {
            Text("복용 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                medicationList.forEach { medication ->
                    MedicationScheduleItem(medication = medication, onTakePill = { onTakePill(medication) }, onRemove = { onRemoveMedication(medication) })
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
                OutlinedTextField(value = medName, onValueChange = { medName = it }, label = { Text("약 이름") })
                OutlinedTextField(value = medTime, onValueChange = { medTime = it }, label = { Text("복용 시간 (예: 09:00)") })
            }
        },
        confirmButton = { Button(onClick = { if (medName.isNotBlank() && medTime.isNotBlank()) onAdd(medName, medTime) }) { Text("추가") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun MedicationStatusCard(takenCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = "정보", tint = Color(0xFF9575CD))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("오늘의 복용 현황", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$takenCount/$totalCount", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Check, contentDescription = "완료", tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun MedicationScheduleItem(medication: Medication, onTakePill: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.Medication, contentDescription = "약",
                    modifier = Modifier.clip(CircleShape).background(if (medication.taken) Color(0xFFE8F5E9) else Color(0xFFE0E0E0)).padding(8.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (medication.taken) {
                    Text("복용 완료", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFF4CAF50), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
                } else {
                    Button(onClick = onTakePill, shape = RoundedCornerShape(8.dp)) { Text("복용") }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray) }
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) { // NavController 추가
    val context = LocalContext.current
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = UserSessionManager.currentUser?.id
        if (userId == null) {
            errorMessage = "로그인된 사용자 정보가 없습니다."
            return@LaunchedEffect
        }
        RetrofitInstance.api.getUserInfo(userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) userInfo = response.body()?.data
                else errorMessage = "사용자 정보 로딩 실패"
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                errorMessage = "네트워크 오류 발생"
            }
        })
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("내 정보", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    errorMessage != null -> Text(errorMessage!!, color = Color.Red)
                    userInfo == null -> CircularProgressIndicator()
                    else -> {
                        InfoRow(label = "이름", value = userInfo!!.name)
                        InfoRow(label = "아이디", value = userInfo!!.username)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 로그아웃 버튼 추가
        Button(
            onClick = {
                UserSessionManager.logout(context)
                navController.navigate(AppDestinations.LOGIN) {
                    // 전체 백스택을 지우고 로그인 화면으로 이동
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("로그아웃")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Text(value)
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = mapOf(
        AppDestinations.HOME to "홈",
        AppDestinations.MEDICATION to "약 관리",
        AppDestinations.SETTINGS to "설정"
    )
    val icons = mapOf(
        AppDestinations.HOME to Icons.Default.Home,
        AppDestinations.MEDICATION to Icons.Default.Info,
        AppDestinations.SETTINGS to Icons.Default.Settings
    )

    NavigationBar {
        items.forEach { (screen, label) ->
            NavigationBarItem(
                icon = { Icon(icons[screen]!!, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == screen,
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
