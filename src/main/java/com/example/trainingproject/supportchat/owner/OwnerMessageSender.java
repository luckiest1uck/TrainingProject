package com.example.trainingproject.supportchat.owner;

public interface OwnerMessageSender {

    OwnerMessageDeliveryResult send(OwnerMessage message);
}
