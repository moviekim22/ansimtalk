package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

// 백그라운드에서 낙상 감지만 수행하는 서비스
class DetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    companion object {
        private const val FREE_FALL_THRESHOLD = 3.0f
        private const val SHOCK_THRESHOLD = 18.0f
        private const val GYRO_SHOCK_THRESHOLD = 10.0f
        private const val IMPACT_TIME_WINDOW_MS = 1200L
        private const val FALL_COOLDOWN_MS = 10000L
        private const val FALL_DETECTION_CHANNEL_ID = "FALL_DETECTION_CHANNEL"
    }

    private var freeFallStartTime = 0L
    private var isFreeFall = false
    private var lastFallDetectedTime = 0L
    private var lastGyroscopeMagnitude = 0.0f

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

    // FullScreenIntent를 사용하여 MainActivity를 즉시 실행하는 함수
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
            .setFullScreenIntent(fullScreenPendingIntent, true) // FullScreen Intent 설정

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createServiceNotification())
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        Log.d("DetectionService", "낙상 감지 서비스가 시작되었습니다.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("DetectionService", "낙상 감지 서비스가 종료되었습니다.")
    }

    private fun createHighPriorityNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALL_DETECTION_CHANNEL_ID,
                "낙상 감지 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "낙상 감지 시 화면을 즉시 켜고 알림을 표시합니다."
                // 이 채널은 화면을 직접 띄우는 용도이므로, 소리나 진동은 생략
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 서비스가 강제 종료되지 않도록 하기 위한 최소한의 알림
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
