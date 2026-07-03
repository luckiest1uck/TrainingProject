package com.example.trainingproject.supportchat.exception;

public final class DuplicateSupportChatMessageException extends SupportChatException {

    public DuplicateSupportChatMessageException() {
        super("Repeated message rejected.");
    }
}
