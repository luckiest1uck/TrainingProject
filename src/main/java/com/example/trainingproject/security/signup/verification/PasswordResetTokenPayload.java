package com.example.trainingproject.security.signup.verification;

public record PasswordResetTokenPayload(String email) implements EmailTokenPayload {}
