package com.example.trainingproject.security.signin.exception;

import java.util.UUID;

public final class SessionNotFoundException extends AuthSecurityException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
