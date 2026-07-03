package com.example.trainingproject.security.endpoint;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.trainingproject.common.util.ClientIpExtractor;
import com.example.trainingproject.openapi.dto.SessionInfo;
import com.example.trainingproject.openapi.dto.UserAuthenticationResponse;
import com.example.trainingproject.openapi.security.api.SessionsApi;
import com.example.trainingproject.security.api.CurrentUserProvider;
import com.example.trainingproject.security.session.management.AuthSessionRequestMetadata;
import com.example.trainingproject.security.session.management.AuthSessionService;
import com.example.trainingproject.security.session.revocation.TokenRevocationService;
import com.example.trainingproject.security.session.token.AuthenticationTokens;
import com.example.trainingproject.security.session.token.RefreshTokenResult;
import com.example.trainingproject.security.session.token.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class SessionsEndpoint implements SessionsApi {

    private static final String X_REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    private final AuthSessionService authSessionService;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService tokenRevocationService;
    private final CurrentUserProvider currentUserProvider;
    private final HttpServletRequest httpRequest;
    private final ClientIpExtractor clientIpExtractor;

    @Override
    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        RefreshTokenResult result = refreshTokenService.refresh(httpRequest, requestMetadata());
        return ResponseEntity.status(result.migratedLegacyToken() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toResponse(result.tokens()));
    }

    @Override
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = X_REFRESH_TOKEN_HEADER, required = false) String xRefreshToken) {
        tokenRevocationService.revokeTokens(xRefreshToken, httpRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/api/v1/auth/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authSessionService.revokeAllForUser(currentUserProvider.getUserId());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/api/v1/auth/sessions")
    public ResponseEntity<List<SessionInfo>> getSessions() {
        return ResponseEntity.ok(authSessionService.listActiveSessionInfos(currentUserProvider.getUserId()));
    }

    @Override
    @DeleteMapping("/api/v1/auth/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        authSessionService.revokeById(sessionId, currentUserProvider.getUserId());
        return ResponseEntity.noContent().build();
    }

    private AuthSessionRequestMetadata requestMetadata() {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = clientIpExtractor.extract(httpRequest);
        return new AuthSessionRequestMetadata(userAgent, ipAddress);
    }

    private static UserAuthenticationResponse toResponse(AuthenticationTokens tokens) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(tokens.accessToken());
        response.setRefreshToken(tokens.refreshToken());
        return response;
    }
}
