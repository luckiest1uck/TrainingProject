package com.example.trainingproject.ratelimit.api;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticatedRequestIdentityProvider {

    Optional<String> findIdentity(HttpServletRequest request);
}
