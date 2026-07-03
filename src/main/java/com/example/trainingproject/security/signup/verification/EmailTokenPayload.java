package com.example.trainingproject.security.signup.verification;

public sealed interface EmailTokenPayload permits EmailVerificationTokenPayload, PasswordResetTokenPayload {

    String email();
}
