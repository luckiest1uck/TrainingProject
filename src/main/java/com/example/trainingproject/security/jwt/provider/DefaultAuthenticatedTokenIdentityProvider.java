package com.example.trainingproject.security.jwt.provider;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.example.trainingproject.ratelimit.api.AuthenticatedRequestIdentityProvider;
import com.example.trainingproject.security.jwt.blacklist.JwtTokenBlacklist;
import com.example.trainingproject.security.jwt.resolver.JwtBearerTokenResolver;
import com.example.trainingproject.security.jwt.resolver.JwtTokenClaims;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultAuthenticatedTokenIdentityProvider implements AuthenticatedRequestIdentityProvider {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtTokenBlacklist jwtTokenBlacklist;

    @Override
    public Optional<String> findIdentity(HttpServletRequest request) {
        try {
            String token = jwtBearerTokenResolver.extract(request);
            jwtTokenBlacklist.validateNotBlacklisted(token);
            return Optional.of(jwtTokenClaims.extractAccessTokenEmail(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }
}
