package com.example.trainingproject.review.messaging.kafka.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.example.trainingproject.review.dto.ReviewCreatedEvent;

public record ReviewCreatedKafkaEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String sourceApp,
        Instant occurredAt,
        String correlationId,
        UUID actorId,
        Payload payload) {

    public ReviewCreatedKafkaEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(sourceApp, "sourceApp");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(payload, "payload");
    }

    public static ReviewCreatedKafkaEvent fromDomainEvent(ReviewCreatedEvent event) {
        Payload payload = new Payload(event.reviewId(), event.productId());
        return new ReviewCreatedKafkaEvent(
                event.eventId(), "review.created", 1, "training-project", event.occurredAt(), null, null, payload);
    }

    public record Payload(UUID reviewId, UUID productId) {
        public Payload {
            Objects.requireNonNull(reviewId, "reviewId");
            Objects.requireNonNull(productId, "productId");
        }
    }
}
