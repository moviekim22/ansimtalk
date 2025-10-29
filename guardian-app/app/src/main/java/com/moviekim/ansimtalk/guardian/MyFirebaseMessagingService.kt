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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새로운 FCM 토큰이 발급되었습니다: $token")
        // TODO: 로그인 상태일 경우, 이 토큰을 서버에 전송하여 갱신하는 로직 구현 필요
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM 메시지 수신 From: ${remoteMessage.from}")
        Log.d(TAG, "FCM 데이터 페이로드: ${remoteMessage.data}")

        var notificationTitle: String? = null
        var notificationBody: String? = null

        // 1. 서버가 'notification' 페이로드를 보냈는지 확인 (주로 앱이 포그라운드일 때)
        remoteMessage.notification?.let {
            notificationTitle = it.title
            notificationBody = it.body
        }

        // 2. 'notification' 페이로드가 없다면, 'data' 페이로드가 있는지 확인
        // (서버가 data 메시지만 보냈을 경우, 앱의 모든 상태에서 호출됨)
        if (remoteMessage.data.isNotEmpty()) {
            // 서버에서 보낸 data의 key 값에 따라 title과 body를 설정합니다.
            // 여기서는 서버가 'title', 'body' 라는 key를 사용했다고 가정합니다.
            notificationTitle = remoteMessage.data["title"]
            notificationBody = remoteMessage.data["body"]
        }

        Log.d(TAG, "최종 알림 제목: $notificationTitle")
        Log.d(TAG, "최종 알림 내용: $notificationBody")

        // 제목이나 내용이 있으면 알림을 생성합니다.
        if (notificationTitle != null || notificationBody != null) {
            sendNotification(notificationTitle, notificationBody)
        }
    }

    private fun sendNotification(title: String?, body: String?) {
        val intent = Intent(this, EmergencyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlag)

        val channelId = "ansimtalk_emergency_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "안심톡 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }
}
