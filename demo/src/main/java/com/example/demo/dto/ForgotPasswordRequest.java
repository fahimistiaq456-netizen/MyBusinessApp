package com.example.demo.dto;

public class ForgotPasswordRequest {
    private String emailOrUsername;

    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }

    public String getEmailOrUsername() {
        return emailOrUsername;
    }

    public void setEmailOrUsername(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }
}
