package com.example.demo.dto;

import com.example.demo.model.User;

public class AuthResponse {
    private boolean success;
    private String message;
    private User user;
    private String resetCode;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public AuthResponse(boolean success, String message, User user, String resetCode) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.resetCode = resetCode;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }
}
