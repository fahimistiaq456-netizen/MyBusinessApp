package com.example.mybusinessapp.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * Manages local user authentication state, session persistence, "Remember Me",
 * and configurable backend server endpoints via SharedPreferences.
 */
class UserSessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "mybusiness_user_session"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_JSON = "key_user_json"
        private const val KEY_REMEMBER_ME = "key_remember_me"
        private const val KEY_SERVER_URL = "key_server_url"

        // Standard connection endpoints
        const val URL_EMULATOR = "http://10.0.2.2:8080/"
        const val URL_USB_REVERSE = "http://127.0.0.1:8080/"

        @Volatile
        private var instance: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return instance ?: synchronized(this) {
                instance ?: UserSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Persist logged in user state and details
     */
    fun saveUser(user: UserModel, rememberMe: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putString(KEY_USER_JSON, gson.toJson(user))
            apply()
        }
    }

    /**
     * Update user profile fields while keeping session active
     */
    fun updateUserProfile(
        fullName: String? = null,
        bio: String? = null,
        phone: String? = null,
        location: String? = null,
        role: String? = null
    ): UserModel? {
        val current = getUser() ?: return null
        val updated = current.copy(
            fullName = fullName ?: current.fullName,
            bio = bio ?: current.bio,
            phone = phone ?: current.phone,
            location = location ?: current.location,
            role = role ?: current.role
        )
        prefs.edit().putString(KEY_USER_JSON, gson.toJson(updated)).apply()
        return updated
    }

    /**
     * Retrieve currently cached user
     */
    fun getUser(): UserModel? {
        val json = prefs.getString(KEY_USER_JSON, null) ?: return null
        return try {
            gson.fromJson(json, UserModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user is logged in and remember-me is active
     */
    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val remember = prefs.getBoolean(KEY_REMEMBER_ME, true)
        return loggedIn && remember && getUser() != null
    }

    /**
     * Clear all session data upon logout
     */
    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_JSON)
            apply()
        }
    }

    /**
     * Get the active backend server URL
     */
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, URL_EMULATOR) ?: URL_EMULATOR
    }

    /**
     * Set and save a new backend server URL
     */
    fun setServerUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = "$cleanUrl/"
        }
        prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
        AuthApiService.BASE_URL = cleanUrl
    }

    /**
     * Get Remember Me preference
     */
    fun isRememberMe(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, true)
    }

    /**
     * Set Remember Me preference
     */
    fun setRememberMe(remember: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
    }
}
