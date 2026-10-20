package com.example.mybusinessapp.auth

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private var apiService: AuthApiService = AuthApiService.create()) {

    fun updateBaseUrl(newBaseUrl: String) {
        apiService = AuthApiService.create(newBaseUrl)
    }

    suspend fun checkServerHealth(baseUrl: String = AuthApiService.BASE_URL): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = AuthApiService.create(baseUrl)
            val res = service.checkHealth()
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun getDemoUser(): UserModel {
        return UserModel(
            id = "demo_user_123",
            username = "business_pro",
            email = "demo@businesssuite.com",
            fullName = "Alex Mercer",
            phone = "+1 (555) 019-2834",
            bio = "Lead Enterprise Architect & Product Director",
            location = "San Francisco, CA",
            role = "Senior Vice President"
        )
    }

    suspend fun register(request: SignUpRequest): Result<UserModel> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.user != null) {
                Result.success(response.body()!!.user!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string()) 
                    ?: response.body()?.message 
                    ?: "Registration failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Ensure backend is running: ${e.localizedMessage}"))
        }
    }

    suspend fun login(request: LoginRequest): Result<UserModel> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.user != null) {
                Result.success(response.body()!!.user!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string()) 
                    ?: response.body()?.message 
                    ?: "Login failed. Please check your credentials."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Ensure backend is running: ${e.localizedMessage}"))
        }
    }

    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): Result<UserModel> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateProfile(userId, request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.user != null) {
                Result.success(response.body()!!.user!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string()) 
                    ?: response.body()?.message 
                    ?: "Profile update failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server: ${e.localizedMessage}"))
        }
    }

    suspend fun forgotPassword(emailOrUsername: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(emailOrUsername.trim()))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string()) 
                    ?: response.body()?.message 
                    ?: "Failed to process recovery request (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Ensure backend is running: ${e.localizedMessage}"))
        }
    }

    suspend fun resetPassword(emailOrUsername: String, otp: String, newPassword: String): Result<UserModel> = withContext(Dispatchers.IO) {
        try {
            val req = ResetPasswordRequest(
                emailOrUsername = emailOrUsername.trim(),
                otp = otp.trim(),
                newPassword = newPassword
            )
            val response = apiService.resetPassword(req)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.user ?: UserModel())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string()) 
                    ?: response.body()?.message 
                    ?: "Password reset failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Ensure backend is running: ${e.localizedMessage}"))
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val parsed = Gson().fromJson(errorBody, AuthResponse::class.java)
            parsed.message
        } catch (e: Exception) {
            null
        }
    }
}
