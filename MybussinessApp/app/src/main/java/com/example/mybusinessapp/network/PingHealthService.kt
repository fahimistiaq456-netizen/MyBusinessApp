package com.example.mybusinessapp.network

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed interface PingStatus {
    object Idle : PingStatus
    object Pinging : PingStatus
    data class Connected(val latencyMs: Long, val endpoint: String, val statusCode: Int = 200) : PingStatus
    data class Unreachable(val reason: String, val endpoint: String) : PingStatus
}

/**
 * Service to execute lightweight HEAD requests to measure server reachability
 * and round-trip latency in milliseconds.
 */
class PingHealthService {

    private val fastPingClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .callTimeout(3500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    suspend fun ping(targetUrl: String): PingStatus = withContext(Dispatchers.IO) {
        var cleanUrl = targetUrl.trim()
        if (cleanUrl.isEmpty()) {
            return@withContext PingStatus.Unreachable("Empty URL", targetUrl)
        }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = "$cleanUrl/"
        }

        // Try health endpoint first, fallback to base URL
        val healthUrl = "${cleanUrl}api/auth/health"
        val request = Request.Builder()
            .url(healthUrl)
            .head()
            .header("User-Agent", "MyBusinessApp-HealthPing/1.0")
            .build()

        val startTime = SystemClock.elapsedRealtime()
        try {
            fastPingClient.newCall(request).execute().use { response ->
                val latency = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1L)
                return@withContext PingStatus.Connected(
                    latencyMs = latency,
                    endpoint = cleanUrl,
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            // If health endpoint fails due to 404 or path issue, try base root URL HEAD
            if (e !is ConnectException && e !is SocketTimeoutException && e !is UnknownHostException) {
                try {
                    val rootRequest = Request.Builder()
                        .url(cleanUrl)
                        .head()
                        .build()
                    val rootStart = SystemClock.elapsedRealtime()
                    fastPingClient.newCall(rootRequest).execute().use { rootResponse ->
                        val latency = (SystemClock.elapsedRealtime() - rootStart).coerceAtLeast(1L)
                        return@withContext PingStatus.Connected(
                            latencyMs = latency,
                            endpoint = cleanUrl,
                            statusCode = rootResponse.code
                        )
                    }
                } catch (_: Exception) {
                    // Fall through to error reporting below
                }
            }

            val reason = when (e) {
                is ConnectException -> "Connection refused"
                is SocketTimeoutException -> "Timed out (>2.5s)"
                is UnknownHostException -> "Host unresolved"
                is IOException -> e.localizedMessage ?: "Network I/O error"
                else -> e.localizedMessage ?: "Unreachable"
            }
            PingStatus.Unreachable(reason, cleanUrl)
        }
    }
}
