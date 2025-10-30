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
    private var freeFallDetected = false
    private var lastFreeFallTime = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (magnitude < 2.0f) {
                freeFallDetected = true
                lastFreeFallTime = System.currentTimeMillis()
            }
            if (magnitude > 15.0f) {
                if (freeFallDetected && (System.currentTimeMillis() - lastFreeFallTime < 1000)) {
                    Log.e("DetectionService", "★★★ 낙상 감지! 전체 화면 알림을 보냅니다. ★★★")
                    // --- ★★★ '낙상 발생 시간'을 인텐트에 담아 보냄 ★★★ ---
                    showFullScreenNotification(System.currentTimeMillis())
                }
                freeFallDetected = false
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

        // 2. 위 인텐트를 실행할 수 있는 '리모컨' 만들기
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. '전체 화면 인텐트'를 포함한 매우 높은 중요도의 알림 생성
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createServiceNotification())
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
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
