package com.example.trainingproject.supportchat.owner;

import java.util.UUID;

public record OwnerMessage(
        UUID conversationId, UUID messageId, String customerName, String customerEmail, String body) {}
