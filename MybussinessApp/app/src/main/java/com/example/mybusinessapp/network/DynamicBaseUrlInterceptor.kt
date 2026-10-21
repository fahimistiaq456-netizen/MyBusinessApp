package com.example.mybusinessapp.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Custom OkHttp Interceptor that dynamically redirects requests at runtime
 * when the endpoint is updated in Settings, without requiring Retrofit reconstruction.
 */
class DynamicBaseUrlInterceptor(
    private val endpointProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        val currentEndpoint = endpointProvider().trim()
        val targetHttpUrl = currentEndpoint.toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        // Dynamically replace scheme, host, and port while preserving original path segments and queries
        val redirectedUrl = originalUrl.newBuilder()
            .scheme(targetHttpUrl.scheme)
            .host(targetHttpUrl.host)
            .port(targetHttpUrl.port)
            .build()

        val redirectedRequest = originalRequest.newBuilder()
            .url(redirectedUrl)
            .build()

        return chain.proceed(redirectedRequest)
    }
}
