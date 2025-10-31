package com.example.myapplication.util

import com.example.myapplication.dto.LoginResponse

object UserManager {
    var currentUser: LoginResponse? = null

    fun isLoggedIn(): Boolean = currentUser != null

    fun login(user: LoginResponse) {
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }
}
