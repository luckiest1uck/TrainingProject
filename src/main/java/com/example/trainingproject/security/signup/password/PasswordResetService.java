package com.example.trainingproject.security.signup.password;

import org.springframework.stereotype.Service;

import com.example.trainingproject.common.turnstile.TurnstileVerificationRequest;
import com.example.trainingproject.common.turnstile.TurnstileVerifier;
import com.example.trainingproject.common.util.EmailNormalizer;
import com.example.trainingproject.security.signup.exception.TimeTokenException;
import com.example.trainingproject.security.signup.verification.EmailVerificationService;
import com.example.trainingproject.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserLookupApi userLookupApi;
    private final EmailVerificationService emailVerificationService;
    private final TurnstileVerifier turnstileVerifier;

    public void requestReset(String email, String turnstileToken) {
        requestReset(email, turnstileToken, null);
    }

    public void requestReset(String email, String turnstileToken, String remoteIp) {
        turnstileVerifier.verify(TurnstileVerificationRequest.forAction(turnstileToken, remoteIp, "forgot_password"));
        String normalizedEmail = EmailNormalizer.normalize(email);
        try {
            if (userLookupApi.findUserByEmail(normalizedEmail).isEmpty()) {
                log.debug("auth.password.forgot.unknown_email");
                return;
            }
            emailVerificationService.sendPasswordResetCode(normalizedEmail);
        } catch (TimeTokenException _) {
            // Swallow cooldown error — returning a distinct response would confirm the email exists.
            log.debug("auth.password.forgot.cooldown");
        }
    }

    public void confirmReset(String token, String newPassword, String turnstileToken) {
        confirmReset(token, newPassword, turnstileToken, null);
    }

    public void confirmReset(String token, String newPassword, String turnstileToken, String remoteIp) {
        turnstileVerifier.verify(TurnstileVerificationRequest.forAction(turnstileToken, remoteIp, "change_password"));
        emailVerificationService.confirmResetPasswordEmailByCode(token, newPassword);
        log.info("auth.password.changed");
    }
}
