package com.example.trainingproject.supportchat.exception;

public final class SupportChatEmailVerificationRequiredException extends SupportChatException {

    public SupportChatEmailVerificationRequiredException() {
        super("Email verification is required.");
    }
}
