package com.example.trainingproject.security.signin.exception;

public final class InvalidCredentialsException extends AuthSecurityException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }

    public InvalidCredentialsException(Throwable cause) {
        super("Invalid credentials", cause);
    }
}
