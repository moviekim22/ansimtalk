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
    private var gyroscope: Sensor? = null // 자이로스코프 센서 추가

    // 낙상 감지 알고리즘을 위한 상수 (재조정됨)
    companion object {
        private const val FREE_FALL_THRESHOLD = 3.0f      // 자유 낙하 문턱값 (m/s^2)
        private const val SHOCK_THRESHOLD = 15.0f         // 충격 문턱값 (m/s^2)
        private const val GYRO_SHOCK_THRESHOLD = 10.0f    // 자이로스코프 충격 (회전) 문턱값 (rad/s)
        private const val IMPACT_TIME_WINDOW_MS = 1200L   // 자유 낙하 후 충격 감지 유효 시간 (ms)
        private const val FALL_COOLDOWN_MS = 10000L       // 낙상 감지 후 쿨다운 시간 (10초)
    }

    private var freeFallStartTime = 0L
    private var isFreeFall = false
    private var lastFallDetectedTime = 0L

    private var lastGyroscopeMagnitude = 0.0f // 최신 자이로스코프 회전 크기 저장
    private var lastGyroscopeTimestamp = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        val now = System.currentTimeMillis()

        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 쿨다운: 마지막 낙상 감지 후 일정 시간 동안은 새로운 감지를 무시
                if (now - lastFallDetectedTime < FALL_COOLDOWN_MS) {
                    isFreeFall = false // 쿨다운 중에는 자유 낙하 상태도 초기화
                    return
                }

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                Log.d("DetectionService", "Accel reading: magnitude = $magnitude, isFreeFall = $isFreeFall")

                // 1. 자유 낙하 시작 감지
                if (!isFreeFall && magnitude < FREE_FALL_THRESHOLD) {
                    isFreeFall = true
                    freeFallStartTime = now
                    Log.d("DetectionService", "자유 낙하 상태 시작!")
                }

                // 2. 자유 낙하 중일 때 로직
                if (isFreeFall) {
                    val timeSinceFreeFallStart = now - freeFallStartTime

                    // 2-1. 충격 감지
                    if (magnitude > SHOCK_THRESHOLD) {
                        if (timeSinceFreeFallStart < IMPACT_TIME_WINDOW_MS) {
                            // 가속도계 충격과 자이로스코프 회전이 동시에 발생했는지 확인
                            if (lastGyroscopeMagnitude > GYRO_SHOCK_THRESHOLD && (now - lastGyroscopeTimestamp < IMPACT_TIME_WINDOW_MS)) {
                                Log.e("DetectionService", "★★★ 낙상 감지! (가속도 충격: ${magnitude}, 자이로 회전: ${lastGyroscopeMagnitude}, 시간: ${timeSinceFreeFallStart}ms) ★★★")
                                showFullScreenNotification(now)
                                lastFallDetectedTime = now // 마지막 감지 시간 업데이트 (쿨다운 시작)
                            } else {
                                Log.d("DetectionService", "가속도 충격 감지, 하지만 자이로스코프 회전 부족 또는 시간 초과")
                            }
                        } else {
                            Log.d("DetectionService", "가속도 충격 감지되었으나 자유 낙하 시간 초과: ${timeSinceFreeFallStart}ms")
                        }
                        isFreeFall = false // 상태 초기화
                    }
                    // 2-2. 타임아웃: 일정 시간 동안 충격이 없으면 상태 초기화
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
                lastGyroscopeTimestamp = now
                Log.d("DetectionService", "Gyro reading: magnitude = ${lastGyroscopeMagnitude}")
            }
        }
    }

    private fun showFullScreenNotification(fallDetectionTime: Long) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SHOW_FALL_DIALOG"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FALL_DETECTED", true)
            // --- ★★★ 낙상 발생 시간을 Long 타입으로 전달 ★★★ ---
            putExtra("FALL_TIME", fallDetectionTime)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "FALL_DETECTION_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("긴급 상황: 낙상 감지!")
            .setContentText("앱을 열어 현재 상태를 확인해주세요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) // 자이로스코프 초기화
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createServiceNotification())
        // 두 센서 모두 감지 속도를 높여 더 정밀한 데이터 수집
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } // 자이로스코프 리스너 등록
        Log.d("DetectionService", "낙상 감지 서비스가 시작되었습니다. (가속도, 자이로스코프 감지 속도: GAME)")
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
                "FALL_DETECTION_CHANNEL",
                "낙상 감지 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "낙상 감지 시 화면을 즉시 켜고 알림을 표시합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(): Notification {
        val channelId = "AnsimTalkServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "안심톡 감지 서비스", NotificationManager.IMPORTANCE_LOW)
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
