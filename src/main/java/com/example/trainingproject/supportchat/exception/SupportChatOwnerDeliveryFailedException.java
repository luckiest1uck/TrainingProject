package com.example.trainingproject.supportchat.exception;

public final class SupportChatOwnerDeliveryFailedException extends SupportChatException {

    public SupportChatOwnerDeliveryFailedException() {
        super("Support chat message could not be delivered to support.");
    }
}
