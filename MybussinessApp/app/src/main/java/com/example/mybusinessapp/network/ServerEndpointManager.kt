package com.example.mybusinessapp.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.serverDataStore: DataStore<Preferences> by preferencesDataStore(name = "server_network_settings")

/**
 * Manages runtime backend endpoint configuration backed by Jetpack DataStore<Preferences>.
 * Provides both reactive Flow<String> for Compose UI and immediate synchronous reading
 * for the OkHttp DynamicBaseUrlInterceptor.
 */
class ServerEndpointManager private constructor(private val context: Context) {

    companion object {
        val KEY_ENDPOINT_URL = stringPreferencesKey("backend_endpoint_url")
        const val DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080/"
        const val DEFAULT_USB_URL = "http://127.0.0.1:8080/"

        @Volatile
        private var instance: ServerEndpointManager? = null

        fun getInstance(context: Context): ServerEndpointManager {
            return instance ?: synchronized(this) {
                instance ?: ServerEndpointManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Thread-safe volatile cache for immediate access in the OkHttp Interceptor thread
    @Volatile
    var currentEndpoint: String = DEFAULT_EMULATOR_URL
        private set

    val endpointFlow: Flow<String> = context.serverDataStore.data
        .map { preferences ->
            preferences[KEY_ENDPOINT_URL] ?: DEFAULT_EMULATOR_URL
        }
        .distinctUntilChanged()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            endpointFlow.collect { url ->
                currentEndpoint = sanitizeUrl(url)
            }
        }
    }

    suspend fun updateEndpoint(newUrl: String) {
        val cleanUrl = sanitizeUrl(newUrl)
        currentEndpoint = cleanUrl
        context.serverDataStore.edit { preferences ->
            preferences[KEY_ENDPOINT_URL] = cleanUrl
        }
    }

    fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        if (clean.isEmpty()) return DEFAULT_EMULATOR_URL
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }
}
