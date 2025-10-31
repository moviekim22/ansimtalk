package com.example.myapplication.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myapplication.dto.LoginResponse

/**
 * 사용자 로그인 세션을 관리하는 싱글톤 객체.
 * SharedPreferences를 사용하여 로그인 정보를 기기에 저장하고 관리합니다.
 */
object UserSessionManager {
    private const val PREFS_NAME = "ansimtalk_session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_LOGIN_ID = "login_id"

    private var prefs: SharedPreferences? = null

    // 메모리에 현재 사용자 정보를 저장하여 빠르게 접근
    var currentUser: LoginResponse? = null
        private set

    // 앱 시작 시 한 번만 호출하여 초기화
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // 저장된 세션 정보를 불러와 메모리에 로드하고, 로그인 여부를 반환
    fun loadSession(): Boolean {
        if (prefs?.getBoolean(KEY_IS_LOGGED_IN, false) == true) {
            val id = prefs?.getLong(KEY_USER_ID, -1L) ?: -1L
            val name = prefs?.getString(KEY_USER_NAME, null)
            val loginId = prefs?.getString(KEY_LOGIN_ID, null)

            if (id != -1L && name != null && loginId != null) {
                currentUser = LoginResponse(id = id, name = name, loginId = loginId)
                UserManager.login(currentUser!!) // 기존 UserManager와의 호환성 유지
                return true
            }
        }
        return false
    }

    // 로그인 시 사용자 정보를 메모리와 SharedPreferences에 저장
    fun login(context: Context, user: LoginResponse) {
        init(context) // SharedPreferences가 초기화되었는지 확인
        currentUser = user
        UserManager.login(user) // 기존 UserManager와의 호환성 유지

        prefs?.edit(commit = true) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_LOGIN_ID, user.loginId)
        }
    }

    // 로그아웃 시 모든 세션 정보를 삭제
    fun logout(context: Context) {
        init(context) // SharedPreferences가 초기화되었는지 확인
        currentUser = null
        UserManager.logout() // 기존 UserManager와의 호환성 유지

        prefs?.edit(commit = true) {
            clear()
        }
    }

    fun isLoggedIn(): Boolean = currentUser != null
}
