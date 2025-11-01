package com.moviekim.ansimtalk.guardian.ui.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.moviekim.ansimtalk.guardian.ui.dto.LoginResponse

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "AnsimTalkEncryptedPrefs", // 암호화된 SharedPreferences 파일 이름
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        const val USER_ID = "user_id"
        const val USER_LOGIN_ID = "user_login_id"
        const val USER_NAME = "user_name"
    }

    /**
     * 로그인 성공 시 사용자 정보를 암호화하여 저장합니다.
     */
    fun saveUser(user: LoginResponse) {
        val editor = prefs.edit()
        editor.putLong(USER_ID, user.id)
        editor.putString(USER_LOGIN_ID, user.loginId)
        editor.putString(USER_NAME, user.name)
        editor.apply()
    }

    /**
     * 저장된 사용자 이름을 가져옵니다. 없으면 null을 반환합니다.
     */
    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    /**
     * 로그아웃 시 저장된 모든 정보를 삭제합니다.
     */
    fun clear() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    /**
     * 로그인 상태를 확인합니다.
     */
    fun isLoggedIn(): Boolean {
        // 사용자 이름이 저장되어 있는지 여부로 로그인 상태를 판단
        return getUserName() != null
    }
}
