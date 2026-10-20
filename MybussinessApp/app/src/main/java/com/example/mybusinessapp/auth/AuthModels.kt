package com.example.mybusinessapp.auth

import com.google.gson.annotations.SerializedName

data class UserModel(
    @SerializedName("id") val id: String? = null,
    @SerializedName("username") val username: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName("fullName") val fullName: String? = "",
    @SerializedName("phone") val phone: String? = "",
    @SerializedName("bio") val bio: String? = "Enterprise Pro Member",
    @SerializedName("location") val location: String? = "New York, USA",
    @SerializedName("role") val role: String? = "Product Manager",
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class SignUpRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null
)

data class LoginRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("role") val role: String?
)

data class ForgotPasswordRequest(
    @SerializedName("emailOrUsername") val emailOrUsername: String
)

data class ResetPasswordRequest(
    @SerializedName("emailOrUsername") val emailOrUsername: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: UserModel? = null,
    @SerializedName("resetCode") val resetCode: String? = null
)
