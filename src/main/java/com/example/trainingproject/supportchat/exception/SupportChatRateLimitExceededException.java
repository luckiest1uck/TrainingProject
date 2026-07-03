package com.example.trainingproject.supportchat.exception;

public final class SupportChatRateLimitExceededException extends SupportChatException {

    public SupportChatRateLimitExceededException() {
        super("Too many support chat messages.");
    }
}
