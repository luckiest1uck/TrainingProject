package com.example.trainingproject.review.exception;

public final class ReviewAccessDeniedException extends ReviewException {

    public ReviewAccessDeniedException() {
        super("Access denied.");
    }
}
