package com.example.trainingproject.security.session.management;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.trainingproject.user.api.UserSessionsRevocationRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class UserSessionRevocationListener {

    private final AuthSessionService authSessionService;

    @EventListener
    void on(UserSessionsRevocationRequestedEvent event) {
        authSessionService.revokeAllForUser(event.userId());
    }
}
