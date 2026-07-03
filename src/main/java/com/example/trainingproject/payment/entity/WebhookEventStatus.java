package com.example.trainingproject.payment.entity;

/** Tracks webhook event processing reliability. Business payment outcome is tracked by {@link PaymentStatus}. */
public enum WebhookEventStatus {
    PROCESSING,
    PROCESSED,
    RETRYABLE_FAILED
}
