package com.example.trainingproject.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trainingproject.payment.entity.StripeWebhookEvent;

@SuppressWarnings("unused") // Spring Data generates implementations for repository methods.
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {}
