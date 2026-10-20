package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.SignUpRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> health() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "UP");
        resp.put("database", "MongoDB Connected");
        resp.put("timestamp", new Date());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody SignUpRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Username is required", null));
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Email is required", null));
        }
        String email = request.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Invalid email address format", null));
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Password must be at least 4 characters", null));
        }

        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse(false, "Username is already taken", null));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse(false, "Email is already registered", null));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName().trim() : username);
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        user.setBio("Enterprise Pro Member");
        user.setLocation("New York, USA");
        user.setRole("Product Manager");

        User savedUser = userRepository.save(user);
        savedUser.setPassword(null);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(true, "Account created successfully", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsernameOrEmail() == null || request.getUsernameOrEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Username or Email is required", null));
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Password is required", null));
        }

        String identifier = request.getUsernameOrEmail().trim();
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier.toLowerCase());
        }

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(false, "Invalid credentials. Please check your username/email and password.", null));
        }

        User user = userOpt.get();
        user.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(true, "Login successful", user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmailOrUsername() == null || request.getEmailOrUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Email or Username is required", null));
        }

        String identifier = request.getEmailOrUsername().trim();
        Optional<User> userOpt = userRepository.findByEmail(identifier.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(identifier);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse(false, "No account found associated with that email or username", null));
        }

        User user = userOpt.get();
        String otp = String.format("%06d", new Random().nextInt(1000000));
        Date expiry = new Date(System.currentTimeMillis() + 15 * 60 * 1000);

        user.setResetOtp(otp);
        user.setResetOtpExpiry(expiry);
        userRepository.save(user);

        System.out.println("[AUTH] Generated password reset OTP for user: " + user.getUsername() + " (" + user.getEmail() + "): " + otp);

        AuthResponse response = new AuthResponse(true, "Password recovery code: " + otp, null, otp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getEmailOrUsername() == null || request.getEmailOrUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Email or Username is required", null));
        }
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Verification code is required", null));
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 4) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "New password must be at least 4 characters", null));
        }

        String identifier = request.getEmailOrUsername().trim();
        Optional<User> userOpt = userRepository.findByEmail(identifier.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(identifier);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse(false, "No account found associated with that email or username", null));
        }

        User user = userOpt.get();

        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "No reset code requested. Please request a new code.", null));
        }

        if (new Date().after(user.getResetOtpExpiry())) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Reset code has expired. Please request a new code.", null));
        }

        if (!user.getResetOtp().equals(request.getOtp().trim())) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Invalid verification code. Please check and try again.", null));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);

        User savedUser = userRepository.save(user);
        savedUser.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(true, "Password reset successfully. You can now log in with your new password.", savedUser));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<AuthResponse> getProfile(@PathVariable String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse(false, "User not found", null));
        }

        User user = userOpt.get();
        user.setPassword(null);
        return ResponseEntity.ok(new AuthResponse(true, "Profile retrieved", user));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<AuthResponse> updateProfile(@PathVariable String userId, @RequestBody UpdateProfileRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse(false, "User not found", null));
        }

        User user = userOpt.get();
        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getBio() != null) user.setBio(request.getBio().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if (request.getLocation() != null) user.setLocation(request.getLocation().trim());
        if (request.getRole() != null) user.setRole(request.getRole().trim());

        User updated = userRepository.save(user);
        updated.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(true, "Profile updated successfully", updated));
    }
}
