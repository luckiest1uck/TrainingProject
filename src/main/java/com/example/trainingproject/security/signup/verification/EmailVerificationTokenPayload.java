package com.example.trainingproject.security.signup.verification;

public record EmailVerificationTokenPayload(String email, EmailRegistrationPayload registration, String encodedPassword)
        implements EmailTokenPayload {}
