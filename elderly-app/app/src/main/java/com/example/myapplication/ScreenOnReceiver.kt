package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.util.ActivityMonitor

/**
 * 화면이 켜지고 사용자가 잠금을 해제하는 시스템 이벤트를 수신하는 리시버.
 */
class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 사용자가 잠금을 해제할 때(ACTION_USER_PRESENT)만 시간을 기록합니다.
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("ScreenOnReceiver", "사용자 잠금 해제 감지. 마지막 활동 시간을 기록합니다.")
            ActivityMonitor.recordUserPresent(context)
        }
    }
}
