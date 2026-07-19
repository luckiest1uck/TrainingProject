package com.example.trainingproject.security.signin.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.trainingproject.common.turnstile.TurnstileVerificationRequest;
import com.example.trainingproject.common.turnstile.TurnstileVerifier;
import com.example.trainingproject.common.util.EmailNormalizer;
import com.example.trainingproject.openapi.dto.UserAuthenticationRequest;
import com.example.trainingproject.security.session.management.AuthSessionRequestMetadata;
import com.example.trainingproject.security.session.token.AuthenticationTokens;
import com.example.trainingproject.security.session.token.SessionTokenService;
import com.example.trainingproject.security.signin.exception.InvalidCredentialsException;
import com.example.trainingproject.security.signin.exception.UserAccountLockedException;
import com.example.trainingproject.security.signin.lockout.LoginAttemptProperties;
import com.example.trainingproject.security.signin.lockout.LoginAttemptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final SessionTokenService sessionTokenService;
    private final TurnstileVerifier turnstileVerifier;
    private final LoginAttemptProperties loginAttemptProperties;

    public AuthenticationTokens authenticate(
            final UserAuthenticationRequest request, final AuthSessionRequestMetadata requestMetadata) {
        turnstileVerifier.verify(TurnstileVerificationRequest.forAction(
                request.getTurnstileToken(), requestMetadata.ipAddress(), "login"));
        UserDetails userDetails = verifyCredentials(request);
        loginAttemptService.resetAfterSuccessfulAuthentication(EmailNormalizer.normalize(request.getEmail()));
        return sessionTokenService.issueForNewSession(userDetails, requestMetadata);
    }

    public UserDetails verifyCredentials(final UserAuthenticationRequest request) {
        String userEmail = EmailNormalizer.normalize(request.getEmail());
        String userPassword = request.getPassword();
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword);

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
                // amazonq-ignore-next-line
                throw new InvalidCredentialsException();
            }

            return userDetails;

        } catch (UsernameNotFoundException | BadCredentialsException exception) {
            loginAttemptService.recordFailure(userEmail);
            throw new InvalidCredentialsException(exception);
        } catch (LockedException exception) {
            log.debug("auth.failed: reason=account_locked");
            throw new UserAccountLockedException(loginAttemptProperties.lockoutDurationMinutes());
        } catch (AuthenticationException exception) {
            log.error("auth.error: exceptionClass={}", exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }
}
