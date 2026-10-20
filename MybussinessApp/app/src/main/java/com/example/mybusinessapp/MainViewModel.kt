package com.example.mybusinessapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybusinessapp.auth.AuthRepository
import com.example.mybusinessapp.auth.LoginRequest
import com.example.mybusinessapp.auth.SignUpRequest
import com.example.mybusinessapp.auth.UpdateProfileRequest
import com.example.mybusinessapp.auth.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardData(
    val totalRevenue: String = "0.00",
    val newOrders: String = "0",
    val customers: String = "0",
    val growth: String = "0"
)

data class Story(val id: String, val name: String)

enum class ServerStatus {
    Connected,
    Offline,
    Checking
}

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private var sessionManager: com.example.mybusinessapp.auth.UserSessionManager? = null

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData = _dashboardData.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories = _stories.asStateFlow()

    private val _isMenuExpanded = MutableStateFlow(false)
    val isMenuExpanded = _isMenuExpanded.asStateFlow()

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus.Checking)
    val serverStatus = _serverStatus.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe = _rememberMe.asStateFlow()

    private val _serverBaseUrl = MutableStateFlow(com.example.mybusinessapp.auth.AuthApiService.BASE_URL)
    val serverBaseUrl = _serverBaseUrl.asStateFlow()

    init {
        loadDashboardData()
        loadStories()
    }

    private fun loadDashboardData() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val newData = DashboardData(
                totalRevenue = "14,230.50",
                newOrders = "128",
                customers = "1,205",
                growth = "12.5"
            )
            _dashboardData.value = newData
        }
    }

    fun addStory() {
        viewModelScope.launch(Dispatchers.IO) {
            val newStory = Story(System.currentTimeMillis().toString(), "New")
            _stories.value = _stories.value + newStory
        }
    }

    private fun loadStories() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            _stories.value = listOf(
                Story("0", "Add"),
                Story("1", "Alice"),
                Story("2", "Bob"),
                Story("3", "Team"),
                Story("4", "Event")
            )
        }
    }

    fun initSession(manager: com.example.mybusinessapp.auth.UserSessionManager) {
        sessionManager = manager
        _rememberMe.value = manager.isRememberMe()
        val savedUrl = manager.getServerUrl()
        _serverBaseUrl.value = savedUrl
        authRepository.updateBaseUrl(savedUrl)
        if (manager.isLoggedIn()) {
            _currentUser.value = manager.getUser()
        }
        checkServerHealth()
    }

    fun setRememberMe(enabled: Boolean) {
        _rememberMe.value = enabled
        sessionManager?.setRememberMe(enabled)
    }

    fun setServerBaseUrl(url: String, onFinished: ((Boolean) -> Unit)? = null) {
        sessionManager?.setServerUrl(url)
        val cleanUrl = sessionManager?.getServerUrl() ?: url
        _serverBaseUrl.value = cleanUrl
        authRepository.updateBaseUrl(cleanUrl)
        checkServerHealth(onFinished)
    }

    fun checkServerHealth(onFinished: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _serverStatus.value = ServerStatus.Checking
            val isOnline = authRepository.checkServerHealth(_serverBaseUrl.value)
            _serverStatus.value = if (isOnline) ServerStatus.Connected else ServerStatus.Offline
            onFinished?.invoke(isOnline)
        }
    }

    fun loginDemoUser(onSuccess: () -> Unit) {
        val demoUser = authRepository.getDemoUser()
        _currentUser.value = demoUser
        sessionManager?.saveUser(demoUser, _rememberMe.value)
        onSuccess()
    }

    // --- Backend Authentication ---

    fun login(
        username: String,
        password: String,
        rememberMe: Boolean = _rememberMe.value,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isBlank() || password.isBlank()) {
            onError("Please enter both username/email and password")
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepository.login(LoginRequest(username.trim(), password))
            _isAuthLoading.value = false

            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    sessionManager?.saveUser(user, rememberMe)
                    _serverStatus.value = ServerStatus.Connected
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Login failed")
                }
            )
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            onError("Username, Email, and Password are required")
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val req = SignUpRequest(
                username = username.trim(),
                email = email.trim(),
                password = password,
                fullName = fullName.trim().ifEmpty { null },
                phone = phone.trim().ifEmpty { null }
            )
            val result = authRepository.register(req)
            _isAuthLoading.value = false

            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    sessionManager?.saveUser(user, true)
                    _serverStatus.value = ServerStatus.Connected
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Registration failed")
                }
            )
        }
    }

    fun updateProfile(
        fullName: String,
        bio: String,
        phone: String,
        location: String,
        role: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        val userId = user?.id
        if (userId.isNullOrBlank() || userId.startsWith("demo_")) {
            val updated = (_currentUser.value ?: UserModel()).copy(
                fullName = fullName,
                bio = bio,
                phone = phone,
                location = location,
                role = role
            )
            _currentUser.value = updated
            sessionManager?.updateUserProfile(fullName, bio, phone, location, role)
            onSuccess()
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val req = UpdateProfileRequest(
                fullName = fullName.trim(),
                bio = bio.trim(),
                phone = phone.trim(),
                location = location.trim(),
                role = role.trim()
            )
            val result = authRepository.updateProfile(userId, req)
            _isAuthLoading.value = false

            result.fold(
                onSuccess = { updated ->
                    _currentUser.value = updated
                    sessionManager?.updateUserProfile(fullName, bio, phone, location, role)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Failed to update profile")
                }
            )
        }
    }

    fun forgotPassword(
        emailOrUsername: String,
        onSuccess: (code: String?, message: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (emailOrUsername.isBlank()) {
            onError("Please enter your email or username")
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepository.forgotPassword(emailOrUsername)
            _isAuthLoading.value = false

            result.fold(
                onSuccess = { response ->
                    onSuccess(response.resetCode, response.message ?: "Recovery code generated")
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Failed to request recovery code")
                }
            )
        }
    }

    fun resetPassword(
        emailOrUsername: String,
        otp: String,
        newPassword: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (emailOrUsername.isBlank() || otp.isBlank() || newPassword.isBlank()) {
            onError("All fields are required")
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepository.resetPassword(emailOrUsername, otp, newPassword)
            _isAuthLoading.value = false

            result.fold(
                onSuccess = {
                    onSuccess("Password reset successfully! You can now log in.")
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Password reset failed")
                }
            )
        }
    }

    fun logout() {
        sessionManager?.clearSession()
        _currentUser.value = null
    }

    fun toggleMenu() { _isMenuExpanded.value = !_isMenuExpanded.value }
    fun closeMenu() { _isMenuExpanded.value = false }
}
