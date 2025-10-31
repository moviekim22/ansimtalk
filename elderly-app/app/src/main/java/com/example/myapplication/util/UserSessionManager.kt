package com.example.myapplication.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myapplication.dto.LoginResponse

object UserSessionManager {
    private const val PREFS_NAME = "ansimtalk_session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_LOGIN_ID = "login_id" // "username" -> "login_id"

    private var prefs: SharedPreferences? = null

    var currentUser: LoginResponse? = null
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun loadSession() {
        if (prefs?.getBoolean(KEY_IS_LOGGED_IN, false) == true) {
            val id = prefs?.getLong(KEY_USER_ID, -1L) ?: -1L
            val name = prefs?.getString(KEY_USER_NAME, null)
            val loginId = prefs?.getString(KEY_LOGIN_ID, null) // "username" -> "loginId"

            if (id != -1L && name != null && loginId != null) {
                currentUser = LoginResponse(id = id, name = name, loginId = loginId) // "username" -> "loginId"
                UserManager.login(currentUser!!) // UserManager와의 호환성 유지
            }
        }
    }

    fun login(context: Context, user: LoginResponse) {
        init(context)
        currentUser = user
        UserManager.login(user)

        // KTX extension function 사용
        prefs?.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_LOGIN_ID, user.loginId) // "username" -> "loginId"
            apply()
        }
    }

    fun logout(context: Context) {
        init(context)
        currentUser = null
        UserManager.logout()

        prefs?.edit {
            clear()
            apply()
        }
    }

    fun isLoggedIn(): Boolean = currentUser != null
}
