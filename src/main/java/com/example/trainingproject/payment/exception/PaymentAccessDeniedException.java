package com.example.trainingproject.payment.exception;

public final class PaymentAccessDeniedException extends PaymentException {

    public PaymentAccessDeniedException() {
        super("Access denied.");
    }
}
