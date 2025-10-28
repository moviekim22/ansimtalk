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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_Service"

    /**
     * 앱이 설치되거나 데이터가 삭제될 때 등, 새로운 FCM 토큰이 생성될 때마다 호출됩니다.
     * 이 토큰을 서버로 보내서 사용자와 매핑하여 저장해야 합니다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새로운 FCM 토큰이 발급되었습니다: $token")

        // TODO: 이 토큰을 서버에 전송하여 저장하는 로직 구현 필요
    }

    /**
     * PUSH 알림 메시지를 수신했을 때 호출됩니다.
     * (앱이 포그라운드에 있을 때, 또는 데이터 메시지를 받았을 때)
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM 메시지 수신 From: ${remoteMessage.from}")

        // 서버에서 보낸 Notification 데이터가 있는지 확인합니다.
        remoteMessage.notification?.let { notification ->
            val title = notification.title
            val body = notification.body

            Log.d(TAG, "알림 제목: $title")
            Log.d(TAG, "알림 내용: $body")

            // 수신된 정보를 바탕으로 시스템 알림을 생성합니다.
            sendNotification(title, body)
        }
    }

    /**
     * 수신된 메시지를 바탕으로 스마트폰 상단에 표시될 시스템 알림을 생성합니다.
     * 이 알림을 탭하면 EmergencyActivity가 열립니다.
     */
    private fun sendNotification(title: String?, body: String?) {
        // 1. EmergencyActivity를 열기 위한 '티켓(Intent)' 생성
        val intent = Intent(this, EmergencyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 기존에 열려있던 다른 화면들을 정리
        }

        // 2. 생성한 티켓을 지금 바로 사용하지 않고, 나중에 사용할 수 있도록 '포장(PendingIntent)'
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, pendingIntentFlag)

        val channelId = "ansimtalk_emergency_channel" // 알림 채널 ID
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 3. Android 8.0 (Oreo) 이상 버전에서는 알림 채널을 먼저 생성해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "안심톡 긴급 알림", // 사용자 설정에 보여질 채널 이름
                NotificationManager.IMPORTANCE_HIGH // 중요도 '높음' (헤드업 알림으로 표시됨)
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 4. 알림을 디자인하고 내용물을 채웁니다.
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 앱 아이콘 (필수)
            .setContentTitle(title) // 알림 제목
            .setContentText(body) // 알림 내용
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 중요도를 높여 상단에 바로 뜨게 함
            .setAutoCancel(true) // 알림을 탭하면 자동으로 사라지게 함
            .setContentIntent(pendingIntent) // (가장 중요!) 알림을 탭했을 때 실행할 '포장된 티켓' 설정

        // 5. 생성된 알림을 시스템에 표시합니다.
        notificationManager.notify(0 /* 알림 고유 ID */, notificationBuilder.build())
    }
}