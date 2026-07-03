package com.example.trainingproject.security.session.token;

public record RefreshTokenResult(AuthenticationTokens tokens, boolean migratedLegacyToken) {}
