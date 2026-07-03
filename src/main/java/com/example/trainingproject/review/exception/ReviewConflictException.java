package com.example.trainingproject.review.exception;

public final class ReviewConflictException extends ReviewException {

    public ReviewConflictException(String message) {
        super(message);
    }

    public ReviewConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
