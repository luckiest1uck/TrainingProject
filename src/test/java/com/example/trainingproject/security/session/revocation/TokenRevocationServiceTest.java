package com.example.trainingproject.security.session.revocation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.config.JwtProperties;
import com.example.trainingproject.security.jwt.exception.JwtTokenException;
import com.example.trainingproject.security.jwt.resolver.JwtBearerTokenResolver;
import com.example.trainingproject.security.jwt.resolver.JwtTokenClaims;
import com.example.trainingproject.security.session.management.AuthSessionService;
import com.example.trainingproject.security.signin.exception.AbsentBearerHeaderException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationService unit tests")
class TokenRevocationServiceTest {

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private HttpServletRequest request;

    private TokenRevocationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new TokenRevocationService(
                jwtTokenBlacklist,
                jwtBearerTokenResolver,
                authSessionService,
                jwtTokenClaims,
                new JwtProperties(
                        "X-Auth-Token",
                        "secret",
                        "refresh-secret",
                        java.time.Duration.ofMinutes(15),
                        java.time.Duration.ofHours(24),
                        "issuer",
                        "audience",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
    }

    @Nested
    @DisplayName("revokeTokens")
    class RevokeTokens {

        @Test
        @DisplayName("revokes and blacklists refresh and access tokens when both are present")
        void revokesAndBlacklistsRefreshAndAccessTokens() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Bearer access.header.payload");
            when(jwtTokenBlacklist.hash("refresh-token")).thenReturn("refresh-hash");
            when(jwtBearerTokenResolver.extract("Bearer access.header.payload")).thenReturn("access.header.payload");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtTokenBlacklist).blacklistRefreshToken("refresh-token");
            verify(jwtTokenBlacklist).blacklist("access.header.payload");
        }

        @Test
        @DisplayName("falls back to the request refresh token when header argument is blank")
        void fallsBackToRefreshTokenFromRequest() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Bearer access.header.payload");
            when(jwtBearerTokenResolver.extract("Bearer access.header.payload")).thenReturn("access.header.payload");
            var sessionId = java.util.UUID.randomUUID();
            when(jwtTokenClaims.extractAccessTokenSessionId("access.header.payload"))
                    .thenReturn(java.util.Optional.of(sessionId));

            service.revokeTokens("  ", request);

            verify(authSessionService).revokeBySessionId(sessionId);
            verify(jwtTokenBlacklist).blacklist("access.header.payload");
            verify(jwtTokenBlacklist, never()).blacklistRefreshToken(any());
        }

        @Test
        @DisplayName("throws when logout without refresh header has no session id in the access token")
        void throwsWhenLogoutWithoutRefreshHeaderHasNoSessionId() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Bearer access.header.payload");
            when(jwtBearerTokenResolver.extract("Bearer access.header.payload")).thenReturn("access.header.payload");
            when(jwtTokenClaims.extractAccessTokenSessionId("access.header.payload"))
                    .thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.revokeTokens(" ", request))
                    .isInstanceOf(JwtTokenException.class)
                    .hasMessageContaining("session");
        }

        @Test
        @DisplayName("still blacklists the access token when refresh token extraction fails")
        void stillBlacklistsAccessTokenWhenRefreshTokenExtractionFails() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Bearer access.header.payload");
            when(jwtBearerTokenResolver.extract("Bearer access.header.payload")).thenReturn("access.header.payload");
            var sessionId = java.util.UUID.randomUUID();
            when(jwtTokenClaims.extractAccessTokenSessionId("access.header.payload"))
                    .thenReturn(java.util.Optional.of(sessionId));

            service.revokeTokens(null, request);

            verify(authSessionService).revokeBySessionId(sessionId);
            verify(jwtTokenBlacklist).blacklist("access.header.payload");
        }

        @Test
        @DisplayName("swallows malformed authorization headers")
        void swallowsMalformedAuthorizationHeaders() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Broken");
            when(jwtTokenBlacklist.hash("refresh-token")).thenReturn("refresh-hash");
            doThrow(new AbsentBearerHeaderException("invalid"))
                    .when(jwtBearerTokenResolver)
                    .extract("Broken");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtTokenBlacklist).blacklistRefreshToken("refresh-token");
        }

        @Test
        @DisplayName("uses configured JWT header when revoking the current access token")
        void usesConfiguredJwtHeaderWhenRevokingCurrentAccessToken() {
            when(request.getHeader("X-Auth-Token")).thenReturn("Bearer access.header.payload");
            when(jwtTokenBlacklist.hash("refresh-token")).thenReturn("refresh-hash");
            when(jwtBearerTokenResolver.extract("Bearer access.header.payload")).thenReturn("access.header.payload");

            service.revokeTokens("refresh-token", request);

            verify(request).getHeader("X-Auth-Token");
            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtTokenBlacklist).blacklistRefreshToken("refresh-token");
            verify(jwtTokenBlacklist).blacklist("access.header.payload");
        }
    }
}
