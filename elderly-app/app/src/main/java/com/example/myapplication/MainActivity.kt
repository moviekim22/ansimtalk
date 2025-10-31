
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // UserSessionManager 초기화
        UserSessionManager.init(this)

        setContent {
            MyApplicationTheme {
                AnsimTalkApp(intent = intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) { // Intent? -> Intent
        super.onNewIntent(intent)
        // 앱이 이미 실행 중일 때 새로운 인텐트(낙상 감지 등)를 받으면 화면을 다시 그림
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

    // 1. 세션 로드 및 시작 화면 결정
    UserSessionManager.loadSession()
    val isFallDetected = intent.getBooleanExtra("FALL_DETECTED", false)
    val startDestination = if (UserSessionManager.isLoggedIn() || isFallDetected) {
        AppDestinations.HOME
    } else {
        AppDestinations.LOGIN
    }

    // 2. 권한 요청 로직
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

    // 3. UI 구성
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
            isFallDetected = isFallDetected
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String,
    isFallDetected: Boolean
) {
    val context = LocalContext.current // LocalContext.current는 @Composable 함수 내에서 호출되어야 함

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(AppDestinations.LOGIN) {
            LoginScreen(navController = navController, onLoginSuccess = { user ->
                UserSessionManager.login(context, user) // context 전달
                navController.navigate(AppDestinations.HOME) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
            })
        }
        composable(AppDestinations.SIGN_UP) { SignUpScreen(navController) }
        composable(AppDestinations.HOME) {
            // 로그인 정보가 없으면 로그인 화면으로 리디렉션 (isFallDetected 경우는 예외)
            if (UserSessionManager.currentUser == null && !isFallDetected) {
                navController.navigate(AppDestinations.LOGIN) {
                    popUpTo(AppDestinations.HOME) { inclusive = true }
                }
            } else {
                HomeScreen(
                    navController = navController,
                    userName = UserSessionManager.currentUser?.name ?: "사용자",
                    isFallDetected = isFallDetected
                )
            }
        }
        composable(AppDestinations.MEDICATION) { MedicationScreen(navController) }
        composable(AppDestinations.SETTINGS) { SettingsScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavController, userName: String, isFallDetected: Boolean) {
    // 낙상 감지 다이얼로그 상태
    var showFallDialog by remember { mutableStateOf(isFallDetected) }

    if (showFallDialog) {
        FallDetectionDialog(onDismiss = { showFallDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(userName = userName)
        EmergencyCallCard()
        MedicationCard(navController, takenCount = 1, totalCount = 2) // Dummy data
    }
}

@Composable
fun FallDetectionDialog(onDismiss: () -> Unit) {
    var countdown by remember { mutableStateOf(30) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        if (countdown == 0) {
            // TODO: 119 신고 로직 구현
            Toast.makeText(context, "자동으로 119에 신고합니다.", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { /* 사용자가 외부를 클릭해도 꺼지지 않도록 비워둠 */ },
        title = {
            Text(
                "낙상 감지!",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "괜찮으신가요? ${countdown}초 후 자동으로 119에 신고됩니다.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("괜찮아요", fontSize = 18.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // TODO: 119 즉시 신고 로직 구현
                    Toast.makeText(context, "즉시 119에 신고합니다.", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("즉시 신고하기", color = Color.Gray)
            }
        }
    )
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
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { if (it.values.all { true }) initiateEmergencyCall(context) }
    )
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("긴급 호출", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("정말로 긴급 호출을 하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("호출") }
            },
            dismissButton = { Button(onClick = { showDialog = false }) { Text("취소") } }
        )
    }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Warning, contentDescription = "긴급 호출", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("긴급 호출", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun initiateEmergencyCall(context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "위치 권한이 없어 호출할 수 없습니다.", Toast.LENGTH_LONG).show()
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
    }.addOnFailureListener {
        Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
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
fun MedicationScreen(navController: NavController) { // Dummy implementation
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF0F2F5)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TopAppBar(
                title = { Text("약 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
        item { MedicationStatusCard(takenCount = 1, totalCount = 2) }
    }
}

@Composable
fun MedicationStatusCard(takenCount: Int, totalCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }

    LaunchedEffect(Unit) {
        val userId = UserSessionManager.currentUser?.id ?: return@LaunchedEffect
        RetrofitInstance.api.getUserInfo(userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) userInfo = response.body()?.data
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) { /* Handle error */ }
        })
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("내 정보", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                userInfo?.let {
                    InfoRow(label = "이름", value = it.name)
                    InfoRow(label = "아이디", value = it.username) // loginId를 username으로 수정
                }
            }
        }
        Button(onClick = {
            UserSessionManager.logout(context)
            navController.navigate(AppDestinations.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }) {
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
    val items = listOf(
        AppDestinations.HOME to Icons.Default.Home,
        AppDestinations.MEDICATION to Icons.Default.Info,
        AppDestinations.SETTINGS to Icons.Default.Settings
    )
    NavigationBar {
        items.forEach { (screen, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = screen) },
                label = { Text(screen) },
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
