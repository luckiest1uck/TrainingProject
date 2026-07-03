package com.example.trainingproject.payment.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record CheckoutPaymentSnapshot(UUID id, @Nullable String providerSessionId) {}
