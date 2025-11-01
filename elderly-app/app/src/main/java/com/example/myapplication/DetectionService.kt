package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.util.ActivityMonitor
import kotlin.math.sqrt

// 백그라운드에서 낙상 감지와 활동 모니터링을 수행하는 서비스
class DetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // --- 미사용 감지를 위한 핸들러 추가 ---
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var activityCheckRunnable: Runnable

    // ScreenOnReceiver 인스턴스 추가
    private val screenOnReceiver = ScreenOnReceiver()

    companion object {
        // 기존 낙상 감지 상수
        private const val FREE_FALL_THRESHOLD = 3.0f
        private const val SHOCK_THRESHOLD = 18.0f
        private const val GYRO_SHOCK_THRESHOLD = 10.0f
        private const val IMPACT_TIME_WINDOW_MS = 1200L
        private const val FALL_COOLDOWN_MS = 10000L
        private const val FALL_DETECTION_CHANNEL_ID = "FALL_DETECTION_CHANNEL"

        // --- 미사용 감지 상수 추가 ---
        private const val ACTIVITY_CHECK_INTERVAL_MS = 10*1000L//임시로 10초마다 체크로 수정//30 * 60 * 1000L // 30분
        private const val INACTIVITY_THRESHOLD_MS = 0.5*60*1000L//임시로 30초로 수정//4 * 60 * 60 * 1000L // 4시간
        private const val INACTIVITY_NOTIFICATION_ID = 3
        private const val INACTIVITY_CHANNEL_ID = "INACTIVITY_CHANNEL"
    }

    private var freeFallStartTime = 0L
    private var isFreeFall = false
    private var lastFallDetectedTime = 0L
    private var lastGyroscopeMagnitude = 0.0f

    // 기존 onSensorChanged 로직 (변경 없음)
    override fun onSensorChanged(event: SensorEvent?) {
        val now = System.currentTimeMillis()
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (now - lastFallDetectedTime < FALL_COOLDOWN_MS) {
                    isFreeFall = false
                    return
                }
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                Log.d("DetectionService", "Accel: $magnitude, Gyro: $lastGyroscopeMagnitude, FreeFall: $isFreeFall")
                if (!isFreeFall && magnitude < FREE_FALL_THRESHOLD) {
                    isFreeFall = true
                    freeFallStartTime = now
                    Log.d("DetectionService", "자유 낙하 상태 시작!")
                }
                if (isFreeFall) {
                    val timeSinceFreeFallStart = now - freeFallStartTime
                    if (magnitude > SHOCK_THRESHOLD) {
                        if (lastGyroscopeMagnitude > GYRO_SHOCK_THRESHOLD) {
                            Log.e("DetectionService", "★★★ 낙상 감지! FullScreenIntent 실행 ★★★")
                            triggerFullScreenActivity()
                            lastFallDetectedTime = now
                        } else {
                            Log.d("DetectionService", "가속도 충격 감지, 하지만 자이로스코프 회전 부족")
                        }
                        isFreeFall = false
                    }
                    else if (timeSinceFreeFallStart > IMPACT_TIME_WINDOW_MS) {
                        Log.d("DetectionService", "자유 낙하 타임아웃, 상태 초기화")
                        isFreeFall = false
                    }
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastGyroscopeMagnitude = sqrt(x * x + y * y + z * z)
            }
        }
    }

    // 기존 triggerFullScreenActivity 로직 (변경 없음)
    private fun triggerFullScreenActivity() {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SHOW_FALL_DIALOG"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("FALL_DETECTED", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, FALL_DETECTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("긴급 상황: 낙상 감지!")
            .setContentText("앱을 열어 현재 상태를 확인해주세요.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createHighPriorityNotificationChannel(notificationManager)
        notificationManager.notify(2, notificationBuilder.build())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        // --- 동적 리시버 등록 추가 ---
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(screenOnReceiver, filter)
        // --- 미사용 감지 로직 초기화 추가 ---
        activityCheckRunnable = Runnable {
            checkUserActivity()
            handler.postDelayed(activityCheckRunnable, ACTIVITY_CHECK_INTERVAL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createServiceNotification())
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        // --- 주기적 활동 체크 시작 추가 ---
        handler.removeCallbacks(activityCheckRunnable) // 혹시 모를 중복 실행 방지
        handler.post(activityCheckRunnable)

        Log.d("DetectionService", "서비스 시작됨. 낙상 감지 및 활동 모니터링 시작.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        // --- 핸들러 콜백 제거 추가 ---
        handler.removeCallbacks(activityCheckRunnable)
        // --- 동적 리시버 해제 추가 ---
        unregisterReceiver(screenOnReceiver)
        Log.d("DetectionService", "서비스 종료됨.")
    }

    // --- START: 미사용 감지 함수 추가 ---
    private fun checkUserActivity() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        // 화면이 켜져 있는지 확인 (API 레벨에 따라 isInteractive 사용)
        if (powerManager.isInteractive) {
            Log.d("DetectionService", "활동 체크: 화면이 켜져 있어 활동 시간 갱신 후 건너뜁니다.")
            ActivityMonitor.recordUserPresent(this) // 화면이 켜져있는 동안 활동 시간 계속 갱신
            return // 알림 체크 로직 실행 안함
        }

        // --- 이하 로직은 화면이 꺼져있을 때만 실행됨 ---
        val lastUserPresentTime = ActivityMonitor.getLastUserPresentTime(this)
        val currentTime = System.currentTimeMillis()
        val inactiveDuration = currentTime - lastUserPresentTime

        // 로그를 초 단위로 변경하여 확인 용이하게 함
        Log.d("DetectionService", "활동 체크: 마지막 활동 후 ${inactiveDuration / 1000}초 경과")

        if (inactiveDuration > INACTIVITY_THRESHOLD_MS) {
            Log.w("DetectionService", "장시간 미사용 감지! 알림을 보냅니다.")
            showInactivityNotification()
            // 알림을 보낸 후, 다시 알림이 울리지 않도록 현재 시간을 기록
            ActivityMonitor.recordUserPresent(this)
        }
    }

    private fun showInactivityNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createInactivityNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SHOW_INACTIVITY_DIALOG" // 액션 이름 지정
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("INACTIVITY_DETECTED", true) // 꼬리표 추가
        }
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, INACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("괜찮으세요?")
            .setContentText("장시간 휴대폰 사용이 없어 알림을 보냅니다.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(INACTIVITY_NOTIFICATION_ID, notification)
    }

    private fun createInactivityNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INACTIVITY_CHANNEL_ID,
                "장시간 미사용 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "장시간 휴대폰 사용이 없을 때 알림을 보냅니다."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 기존 createHighPriorityNotificationChannel 로직 (변경 없음)
    private fun createHighPriorityNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALL_DETECTION_CHANNEL_ID,
                "낙상 감지 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "낙상 감지 시 화면을 즉시 켜고 알림을 표시합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 기존 createServiceNotification 로직 (변경 없음)
    private fun createServiceNotification(): Notification {
        val channelId = "AnsimTalkServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "안심톡 감지 서비스", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("안심톡")
            .setContentText("안전 감지 기능이 백그라운드에서 실행 중입니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
