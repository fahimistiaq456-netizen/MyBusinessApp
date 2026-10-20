package com.example.demo.dto;

public class ResetPasswordRequest {
    private String emailOrUsername;
    private String otp;
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String emailOrUsername, String otp, String newPassword) {
        this.emailOrUsername = emailOrUsername;
        this.otp = otp;
        this.newPassword = newPassword;
    }

    public String getEmailOrUsername() {
        return emailOrUsername;
    }

    public void setEmailOrUsername(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
