package com.example.trainingproject.security.session.revocation;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.resolver.JwtBearerTokenResolver;
import com.example.trainingproject.security.jwt.resolver.JwtTokenClaims;
import com.example.trainingproject.security.session.management.AuthSessionService;
import com.example.trainingproject.security.signin.exception.AbsentBearerHeaderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final AuthSessionService authSessionService;

    public void revokeTokens(String refreshTokenHeader, HttpServletRequest request) {
        java.util.Optional<String> accessToken = resolveAccessToken(request);
        resolveRefreshToken(refreshTokenHeader)
                .ifPresentOrElse(
                        this::revokeRefreshToken, () -> accessToken.ifPresent(this::revokeSessionFromAccessToken));
        accessToken.ifPresent(this::blacklistAccessToken);
    }

    private java.util.Optional<String> resolveRefreshToken(String refreshTokenHeader) {
        if (StringUtils.hasText(refreshTokenHeader)) {
            return java.util.Optional.of(refreshTokenHeader);
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<String> resolveAccessToken(HttpServletRequest request) {
        try {
            return java.util.Optional.of(jwtBearerTokenResolver.extract(request));
        } catch (AbsentBearerHeaderException _) {
            return java.util.Optional.empty();
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        authSessionService.revokeByRefreshTokenHash(jwtTokenBlacklist.hash(refreshToken));
        jwtTokenBlacklist.blacklistRefreshToken(refreshToken);
    }

    private void revokeSessionFromAccessToken(String accessToken) {
        jwtTokenClaims.extractAccessTokenSessionId(accessToken).ifPresent(authSessionService::revokeBySessionId);
    }

    private void blacklistAccessToken(String accessToken) {
        try {
            jwtTokenBlacklist.blacklist(accessToken);
        } catch (AbsentBearerHeaderException ex) {
            log.debug("auth.logout.token_error: header={} reason={}", HttpHeaders.AUTHORIZATION, ex.getMessage());
        }
    }
}
