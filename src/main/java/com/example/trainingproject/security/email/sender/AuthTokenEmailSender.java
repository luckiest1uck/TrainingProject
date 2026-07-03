package com.example.trainingproject.security.email.sender;

public interface AuthTokenEmailSender {

    void sendTemporaryCode(String email, String message);
}
