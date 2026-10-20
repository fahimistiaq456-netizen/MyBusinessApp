package com.example.mybusinessapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybusinessapp.MainViewModel
import com.example.mybusinessapp.ui.theme.GradientBurst
import com.example.mybusinessapp.ui.theme.GradientSunset

private enum class RecoveryStep {
    REQUEST_CODE,
    RESET_PASSWORD,
    SUCCESS
}

@Composable
fun ForgotPasswordScreen(
    viewModel: MainViewModel,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var currentStep by remember { mutableStateOf(RecoveryStep.REQUEST_CODE) }
    var emailOrUsername by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var receivedCode by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                        .clickable {
                            if (currentStep == RecoveryStep.RESET_PASSWORD) {
                                currentStep = RecoveryStep.REQUEST_CODE
                                errorMessage = null
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Text(
                    text = "Password Recovery",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        brush = GradientSunset
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // Main Glass Card
            FluidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(GradientBurst, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentStep) {
                                RecoveryStep.REQUEST_CODE -> Icons.Rounded.LockReset
                                RecoveryStep.RESET_PASSWORD -> Icons.Rounded.VpnKey
                                RecoveryStep.SUCCESS -> Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Step Title & Subtitle
                    when (currentStep) {
                        RecoveryStep.REQUEST_CODE -> {
                            Text(
                                text = "Forgot Your Password?",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onBackground
                                )
                            )
                            Text(
                                text = "Enter your registered email or username. We will generate a secure 6-digit verification code to reset your password.",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                        RecoveryStep.RESET_PASSWORD -> {
                            Text(
                                text = "Verify & Reset",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onBackground
                                )
                            )
                            Text(
                                text = "Enter the 6-digit recovery code and choose a secure new password.",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                        RecoveryStep.SUCCESS -> {
                            Text(
                                text = "Password Reset Complete!",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            )
                            Text(
                                text = "Your password has been successfully updated in MongoDB. You can now log in with your new credentials.",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    // Informational Message (e.g. OTP display helper)
                    if (infoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Info, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = infoMessage ?: "",
                                        color = colors.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (receivedCode != null && otpCode.isBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Tap to autofill code: $receivedCode",
                                            color = colors.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.primary.copy(alpha = 0.15f))
                                                .clickable { otpCode = receivedCode ?: "" }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error Message
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.error.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = colors.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = colors.error,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Form Fields by Step
                    when (currentStep) {
                        RecoveryStep.REQUEST_CODE -> {
                            RecoveryTextField(
                                value = emailOrUsername,
                                onValueChange = {
                                    emailOrUsername = it
                                    errorMessage = null
                                },
                                label = "Email or Username",
                                icon = Icons.Rounded.Email
                            )

                            Button(
                                onClick = {
                                    if (emailOrUsername.isBlank()) {
                                        errorMessage = "Please enter your email or username"
                                        return@Button
                                    }
                                    errorMessage = null
                                    viewModel.forgotPassword(
                                        emailOrUsername = emailOrUsername,
                                        onSuccess = { code, msg ->
                                            receivedCode = code
                                            otpCode = code ?: ""
                                            infoMessage = "Recovery code sent: $code"
                                            currentStep = RecoveryStep.RESET_PASSWORD
                                        },
                                        onError = { msg -> errorMessage = msg }
                                    )
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GradientBurst)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                    } else {
                                        Text("GET RECOVERY CODE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        RecoveryStep.RESET_PASSWORD -> {
                            RecoveryTextField(
                                value = otpCode,
                                onValueChange = {
                                    otpCode = it
                                    errorMessage = null
                                },
                                label = "6-Digit Verification Code",
                                icon = Icons.Rounded.Key
                            )

                            RecoveryTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    errorMessage = null
                                },
                                label = "New Password (min 4 chars)",
                                icon = Icons.Rounded.Lock,
                                isPassword = true,
                                passwordVisible = newPasswordVisible,
                                onTogglePassword = { newPasswordVisible = !newPasswordVisible }
                            )

                            RecoveryTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    errorMessage = null
                                },
                                label = "Confirm New Password",
                                icon = Icons.Rounded.LockReset,
                                isPassword = true,
                                passwordVisible = confirmPasswordVisible,
                                onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible }
                            )

                            Button(
                                onClick = {
                                    if (otpCode.isBlank()) {
                                        errorMessage = "Please enter the verification code"
                                        return@Button
                                    }
                                    if (newPassword.isBlank() || newPassword.length < 4) {
                                        errorMessage = "Password must be at least 4 characters"
                                        return@Button
                                    }
                                    if (newPassword != confirmPassword) {
                                        errorMessage = "Passwords do not match"
                                        return@Button
                                    }

                                    errorMessage = null
                                    viewModel.resetPassword(
                                        emailOrUsername = emailOrUsername,
                                        otp = otpCode,
                                        newPassword = newPassword,
                                        onSuccess = {
                                            infoMessage = null
                                            currentStep = RecoveryStep.SUCCESS
                                        },
                                        onError = { msg -> errorMessage = msg }
                                    )
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GradientBurst)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                    } else {
                                        Text("RESET PASSWORD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        RecoveryStep.SUCCESS -> {
                            Button(
                                onClick = onResetSuccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GradientBurst)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("PROCEED TO LOG IN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Return to login link
                    if (currentStep != RecoveryStep.SUCCESS) {
                        Text(
                            text = "Back to Log In",
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onBack() }
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RecoveryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colors.onSurfaceVariant, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Toggle Password",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.25f),
            focusedTextColor = colors.onBackground,
            unfocusedTextColor = colors.onBackground,
            cursorColor = colors.primary
        ),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
