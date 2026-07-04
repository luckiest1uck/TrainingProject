package com.example.trainingproject.security.session.revocation;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.config.JwtProperties;
import com.example.trainingproject.security.jwt.exception.JwtTokenException;
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
    private final AuthSessionService authSessionService;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtProperties jwtProperties;

    public void revokeTokens(String refreshTokenHeader, HttpServletRequest request) {
        if (StringUtils.hasText(refreshTokenHeader)) {
            revokeRefreshToken(refreshTokenHeader);
        } else {
            revokeCurrentSession(request);
        }
        revokeAccessToken(request);
    }

    private void revokeCurrentSession(HttpServletRequest request) {
        try {
            String accessToken = jwtBearerTokenResolver.extract(request.getHeader(jwtProperties.header()));
            var sessionId = jwtTokenClaims
                    .extractAccessTokenSessionId(accessToken)
                    .orElseThrow(() -> new JwtTokenException("Access token session is missing"));
            authSessionService.revokeBySessionId(sessionId);
        } catch (AbsentBearerHeaderException ex) {
            log.debug("auth.logout.session_error: header={} reason={}", jwtProperties.header(), ex.getMessage());
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        authSessionService.revokeByRefreshTokenHash(jwtTokenBlacklist.hash(refreshToken));
        jwtTokenBlacklist.blacklistRefreshToken(refreshToken);
    }

    private void revokeAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProperties.header());
        if (!StringUtils.hasText(authHeader)) {
            return;
        }

        try {
            jwtTokenBlacklist.blacklist(jwtBearerTokenResolver.extract(authHeader));
        } catch (AbsentBearerHeaderException ex) {
            log.debug("auth.logout.token_error: header={} reason={}", jwtProperties.header(), ex.getMessage());
        }
    }
}
