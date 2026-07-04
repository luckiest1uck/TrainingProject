package com.example.trainingproject.security.session.token;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingproject.common.audit.Identifiable;
import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.exception.JwtTokenBlacklistedException;
import com.example.trainingproject.security.jwt.provider.JwtAccountStatusValidator;
import com.example.trainingproject.security.jwt.resolver.JwtBearerTokenResolver;
import com.example.trainingproject.security.jwt.resolver.JwtTokenClaims;
import com.example.trainingproject.security.session.entity.AuthSessionEntity;
import com.example.trainingproject.security.session.management.AuthSessionRequestMetadata;
import com.example.trainingproject.security.session.management.AuthSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final UserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;
    private final SessionTokenService sessionTokenService;

    @Transactional
    public RefreshTokenResult refresh(HttpServletRequest request, AuthSessionRequestMetadata requestMetadata) {
        log.debug("auth.token.refreshing");
        String rawToken = jwtBearerTokenResolver.extract(request);
        jwtTokenBlacklist.validateNotBlacklisted(rawToken);
        String hash = jwtTokenBlacklist.hash(rawToken);

        AuthSessionEntity session;
        try {
            session = authSessionService.findActiveByHash(hash);
        } catch (JwtTokenBlacklistedException ex) {
            if (jwtTokenClaims.isSessionManagedRefreshToken(rawToken)) {
                jwtTokenClaims
                        .extractRefreshTokenSessionId(rawToken)
                        .ifPresent(authSessionService::revokeAllForCompromisedUserBySessionId);
                throw ex;
            }
            log.warn("auth.token.refresh_legacy_migrate: reason=token_invalidated");
            String userEmail = extractRefreshTokenEmail(rawToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            JwtAccountStatusValidator.requireActive(userDetails);
            var response = sessionTokenService.migrateLegacyRefreshToken(userDetails, rawToken, requestMetadata);
            log.info("auth.token.refresh_legacy_migrated");
            return new RefreshTokenResult(response, true);
        }

        String userEmail = extractRefreshTokenEmail(rawToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        JwtAccountStatusValidator.requireActive(userDetails);
        requireSessionOwner(session, userDetails);
        var response = sessionTokenService.rotateSessionTokens(session, hash, userDetails);
        log.debug("auth.token.refreshed");
        return new RefreshTokenResult(response, false);
    }

    private void requireSessionOwner(AuthSessionEntity session, UserDetails userDetails) {
        if (userDetails instanceof Identifiable user && session.getUserId().equals(user.getId())) {
            return;
        }
        String logMessage = "auth.token.refresh_session_owner_mismatch: sessionId={}";
        log.warn(logMessage, AuthSessionService.maskSessionId(session.getId()));

        authSessionService.revokeAllForCompromisedUserBySessionId(session.getId());
        throw new JwtTokenBlacklistedException("Refresh token session owner mismatch");
    }

    private String extractRefreshTokenEmail(String rawToken) {
        return jwtTokenClaims.extractRefreshTokenEmail(rawToken);
    }
}
