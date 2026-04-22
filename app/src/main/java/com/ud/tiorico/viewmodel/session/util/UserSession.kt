package com.ud.toolloop.viewmodel.util

import android.content.Context
import androidx.core.content.edit

class UserSession(context: Context) {
    private val prefs = context.getSharedPreferences(
        UserDataSession.SESSION_NAME, Context.MODE_PRIVATE)

    fun saveUser(idUser: String, email: String) {
        prefs.edit().apply {
            putString(UserDataSession.KEY_USER_ID, idUser)
            putString(UserDataSession.KEY_EMAIL, email)
            putBoolean(UserDataSession.KEY_IS_LOGGED, true)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit { clear() }
    }

    fun getUserId(): String? = prefs.getString(UserDataSession.KEY_USER_ID, null)
    fun getEmail(): String? = prefs.getString(UserDataSession.KEY_EMAIL, null)
    fun isLogged(): Boolean = prefs.getBoolean(UserDataSession.KEY_IS_LOGGED, false)
}