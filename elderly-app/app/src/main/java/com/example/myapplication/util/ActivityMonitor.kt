package com.example.myapplication.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 사용자의 마지막 활동 시간(화면 잠금 해제)을 기록하고 관리하는 객체.
 */
object ActivityMonitor {
    private const val PREFS_NAME = "ansimtalk_activity_monitor_prefs"
    private const val KEY_LAST_USER_PRESENT_TIME = "last_user_present_time"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 사용자가 잠금을 해제한 현재 시간을 기록합니다.
     */
    fun recordUserPresent(context: Context) {
        getPrefs(context).edit {
            putLong(KEY_LAST_USER_PRESENT_TIME, System.currentTimeMillis())
        }
    }

    /**
     * 마지막으로 사용자가 잠금을 해제한 시간을 가져옵니다.
     * 기록이 없는 경우, 현재 시간을 반환하여 오검출을 방지합니다.
     */
    fun getLastUserPresentTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_USER_PRESENT_TIME, System.currentTimeMillis())
    }
}
