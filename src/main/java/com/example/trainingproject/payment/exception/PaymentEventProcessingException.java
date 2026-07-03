package com.example.trainingproject.payment.exception;

public final class PaymentEventProcessingException extends PaymentException {

    public PaymentEventProcessingException() {
        super("Stripe webhook signature verification failed.");
    }
}
