package com.example.mybusinessapp.auth

import com.example.mybusinessapp.network.DynamicBaseUrlInterceptor
import com.example.mybusinessapp.network.ServerEndpointManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<AuthResponse>

    @PUT("api/auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<AuthResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<AuthResponse>

    @GET("api/auth/health")
    suspend fun checkHealth(): Response<Map<String, Any>>

    companion object {
        var BASE_URL = ServerEndpointManager.DEFAULT_EMULATOR_URL

        @Volatile
        private var sharedService: AuthApiService? = null

        /**
         * Creates or retrieves the singleton AuthApiService configured with DynamicBaseUrlInterceptor.
         * Requests are dynamically redirected at runtime when settings change, eliminating the need
         * to rebuild Retrofit instances.
         */
        fun create(endpointProvider: (() -> String)? = null): AuthApiService {
            return sharedService ?: synchronized(this) {
                sharedService ?: buildService(endpointProvider).also { sharedService = it }
            }
        }

        private fun buildService(endpointProvider: (() -> String)?): AuthApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)

            // Dynamic base-url switching interceptor
            if (endpointProvider != null) {
                clientBuilder.addInterceptor(DynamicBaseUrlInterceptor(endpointProvider))
            } else {
                clientBuilder.addInterceptor(DynamicBaseUrlInterceptor { BASE_URL })
            }

            val client = clientBuilder.build()
            val initialUrl = endpointProvider?.invoke() ?: BASE_URL
            val validUrl = if (initialUrl.endsWith("/")) initialUrl else "$initialUrl/"

            return Retrofit.Builder()
                .baseUrl(validUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)
        }
    }
}
