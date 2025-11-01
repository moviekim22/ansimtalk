package com.moviekim.ansimtalk.guardian.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveEmail(email: String) {
        with(sharedPreferences.edit()) {
            putString("user_email", email)
            apply()
        }
    }

    fun getEmail(): String? {
        return sharedPreferences.getString("user_email", null)
    }

    fun clearEmail() {
        with(sharedPreferences.edit()) {
            remove("user_email")
            apply()
        }
    }
}
