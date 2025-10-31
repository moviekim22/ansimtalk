
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
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.myapplication.network.StatusRequest
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.UserManager
import com.google.android.gms.location.LocationServices
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
        GuardianContactManager.saveGuardianPhone(this, "010-1234-5678") // 테스트용 보호자 번호
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
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.all { it.value }) {
                Log.d("AnsimTalkApp", "모든 권한이 승인되었습니다. 서비스를 시작합니다.")
                val serviceIntent = Intent(context, DetectionService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Toast.makeText(context, "백그라운드 감지를 위해 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val medicationList = remember {
        mutableStateListOf(
            Medication(name = "철분약", time = "08:00", taken = true),
            Medication(name = "당뇨약", time = "12:00", taken = false),
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = AppDestinations.LOGIN,
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
    medicationList: List<Medication>,
    onAddMedication: (String, String) -> Unit,
    onTakePill: (Medication) -> Unit,
    onRemoveMedication: (Medication) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.LOGIN) {
            LoginScreen(navController = navController, onLoginSuccess = {
                UserManager.login(it)
                navController.navigate(AppDestinations.HOME) {
                    popUpTo(AppDestinations.LOGIN) { inclusive = true }
                }
            })
        }
        composable(AppDestinations.SIGN_UP) { SignUpScreen(navController) }
        composable(AppDestinations.HOME) {
            HomeScreen(navController = navController, userName = UserManager.currentUser?.name ?: "사용자", medicationList = medicationList)
        }
        composable(AppDestinations.SAFETY_CHECK) { SafetyCheckScreen(navController) }
        composable(AppDestinations.MEDICATION) {
            MedicationScreen(navController, medicationList, onAddMedication, onTakePill, onRemoveMedication)
        }
        composable(AppDestinations.SETTINGS) { SettingsScreen() }
    }
}

@Composable
fun HomeScreen(navController: NavController, userName: String, medicationList: List<Medication>) {
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
        Header(userName = userName)
        EmergencyCallCard()
        SafetyCheckCard(navController)
        MedicationCard(navController, takenCount = takenPills, totalCount = totalPills)
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
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = "프로필",
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun EmergencyCallCard() {
    var showEmergencyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.values.all { it }) { // 모든 권한이 승인되었는지 확인
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
                        val permissionsToRequest = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        // 권한 요청
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("호출")
                }
            },
            dismissButton = {
                Button(onClick = { showEmergencyDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Button(
        onClick = { showEmergencyDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // Increased height
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F), // Red color
            contentColor = Color.White
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning, // Warning icon
                contentDescription = "긴급 호출 아이콘",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "긴급 호출",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun initiateEmergencyCall(context: Context) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "위치 권한이 없어 호출을 할 수 없습니다.", Toast.LENGTH_LONG).show()
        return
    }

    val userId = UserManager.currentUser?.id
    if (userId == null) {
        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        return
    }

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            val latitude = location?.latitude ?: 0.0
            val longitude = location?.longitude ?: 0.0

            val eventRequest = EventRequest(
                userId = userId,
                eventType = EventType.EMERGENCY_CALL,
                latitude = latitude,
                longitude = longitude
            )

            RetrofitInstance.api.sendEmergencyEvent(eventRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("EmergencyCall", "긴급 호출 성공")
                        Toast.makeText(context, "긴급 호출이 성공적으로 전송되었습니다.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e("EmergencyCall", "긴급 호출 실패: ${response.code()}")
                        Toast.makeText(context, "긴급 호출 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("EmergencyCall", "네트워크 오류", t)
                    Toast.makeText(context, "네트워크 오류로 긴급 호출에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        .addOnFailureListener { e ->
            Log.e("EmergencyCall", "위치 정보 가져오기 실패", e)
            Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
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
                Button(onClick = { showConfirmationDialog = false; navController.popBackStack() }) {
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
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") } },
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
                    Text("보호자에게 알려드립니다.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    MoodOption(text = "좋아요", icon = Icons.Outlined.SentimentVerySatisfied, color = Color(0xFFE8F5E9), isSelected = selectedMood == "좋아요") { selectedMood = "좋아요" }
                    MoodOption(text = "보통이에요", icon = Icons.Outlined.SentimentNeutral, color = Color(0xFFFFF9C4), isSelected = selectedMood == "보통이에요") { selectedMood = "보통이에요" }
                    MoodOption(text = "안 좋아요", icon = Icons.Outlined.SentimentDissatisfied, color = Color(0xFFFFEBEE), isSelected = selectedMood == "안 좋아요") { selectedMood = "안 좋아요" }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            selectedMood?.let { mood ->
                                val userId = UserManager.currentUser?.id ?: return@let
                                val request = StatusRequest(userId = userId, status = mood)
                                RetrofitInstance.api.sendStatus(request).enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        if (response.isSuccessful) {
                                            showConfirmationDialog = true
                                        } else {
                                            // Handle error
                                        }
                                    }
                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        // Handle failure
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
                if (totalCount > 0 && takenCount == totalCount) {
                    Text("모두 복용 완료!", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("복용 진행 중", color = Color.Gray)
                }
            }
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White),
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
fun SettingsScreen() {
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = UserManager.currentUser?.id
        if (userId == null) {
            errorMessage = "로그인이 필요합니다."
            return@LaunchedEffect
        }

        RetrofitInstance.api.getUserInfo(userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    userInfo = response.body()!!.data
                } else {
                    errorMessage = "사용자 정보를 불러오는데 실패했습니다."
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                errorMessage = "네트워크 오류가 발생했습니다."
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

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = currentRoute == AppDestinations.HOME,
            onClick = { navController.navigate(AppDestinations.HOME) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "안부 확인") },
            label = { Text("안부 확인") },
            selected = currentRoute == AppDestinations.SAFETY_CHECK,
            onClick = { navController.navigate(AppDestinations.SAFETY_CHECK) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Info, contentDescription = "약 관리") },
            label = { Text("약 관리") },
            selected = currentRoute == AppDestinations.MEDICATION,
            onClick = { navController.navigate(AppDestinations.MEDICATION) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            selected = currentRoute == AppDestinations.SETTINGS,
            onClick = { navController.navigate(AppDestinations.SETTINGS) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } }
        )
    }
}
