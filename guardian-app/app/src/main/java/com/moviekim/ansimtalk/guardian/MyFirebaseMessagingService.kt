package com.moviekim.ansimtalk.guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moviekim.ansimtalk.guardian.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_Service"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새로운 FCM 토큰이 발급되었습니다: $token")
        // TODO: 로그인 상태일 경우, 이 토큰을 서버에 전송하여 갱신하는 로직 구현 필요
    }

    /**
     * FCM 메시지를 수신했을 때 호출됩니다.
     * 서버에서 보내는 데이터 페이로드("data")를 처리하여 긴급 상황 알림을 생성합니다.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM 메시지 수신 From: ${remoteMessage.from}")

        // data 페이로드가 있는지 확인합니다. 서버는 항상 data 페이로드에 정보를 담아 보냅니다.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "FCM 데이터 페이로드: ${remoteMessage.data}")

            // 1. 데이터 페이로드에서 정보 추출
            val eventType = remoteMessage.data["eventType"]
            // Double 파싱 실패 시 null이 되도록 toDoubleOrNull 사용
            val latitude = remoteMessage.data["latitude"]?.toDoubleOrNull()
            val longitude = remoteMessage.data["longitude"]?.toDoubleOrNull()
            val elderlyName = remoteMessage.data["elderlyName"]

            // 2. 알림을 클릭했을 때 실행될 EmergencyActivity로 정보를 전달할 Intent 생성
            val intent = Intent(this, EmergencyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 기존 액티비티 스택을 정리
                putExtra("eventType", eventType)
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
                putExtra("elderlyName", elderlyName)
            }

            // 3. 알림의 제목과 본문 가져오기
            // 서버에서 보낸 notification 객체가 있으면 그 값을 사용하고, 없으면 data 페이로드의 값을 사용
            val title = remoteMessage.notification?.title ?: "🚨 긴급 상황 발생!"
            val body = remoteMessage.notification?.body ?: "${elderlyName ?: "어르신"}님에게 도움이 필요합니다! 앱을 확인해주세요."

            // 4. 사용자에게 보여줄 알림 생성
            sendNotification(title, body, intent)

        } else {
            // 데이터 페이로드가 없는 일반 알림의 경우 (예: 공지사항)
            remoteMessage.notification?.let {
                Log.d(TAG, "FCM 알림 페이로드만 수신: ${it.title} / ${it.body}")
                val intent = Intent(this, MainActivity::class.java) // 일반 알림은 MainActivity로
                sendNotification(it.title, it.body, intent)
            }
        }
    }

    /**
     * 수신한 정보를 바탕으로 사용자에게 시스템 알림을 표시합니다.
     * @param title 알림의 제목
     * @param body 알림의 내용
     * @param intent 알림을 클릭했을 때 실행될 Intent
     */
    private fun sendNotification(title: String?, body: String?, intent: Intent) {
        // 알림 클릭 시 실행될 PendingIntent 설정
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        // 매번 다른 requestCode를 주어 Intent의 extra 데이터가 갱신되도록 함
        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, pendingIntentFlag)

        // 알림 채널 ID
        val channelId = "ansimtalk_emergency_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) 이상에서는 알림 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "안심톡 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH // 헤드업 알림을 위해 HIGH로 설정
            ).apply {
                description = "어르신에게 긴급 상황 발생 시 수신되는 알림입니다."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 빌더를 사용하여 알림 구성
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: 앱에 맞는 아이콘으로 변경 필요
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 헤드업 알림을 위해 HIGH로 설정
            .setAutoCancel(true) // 탭하면 자동으로 사라지도록 설정
            .setContentIntent(pendingIntent) // 클릭 시 실행할 Intent

        // 알림 표시. ID를 다르게 해야 여러 알림이 쌓임.
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
