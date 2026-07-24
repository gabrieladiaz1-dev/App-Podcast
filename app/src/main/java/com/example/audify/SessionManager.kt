package com.example.audify

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS_NAME = "audify_session"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_USER_ID = "user_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(email: String, userId: String = "") {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
