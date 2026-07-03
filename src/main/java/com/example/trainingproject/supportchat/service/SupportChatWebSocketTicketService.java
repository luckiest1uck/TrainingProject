package com.example.trainingproject.supportchat.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.trainingproject.security.api.SupportChatWebSocketTicketIssuer;
import com.example.trainingproject.security.api.dto.CurrentUserSnapshot;
import com.example.trainingproject.user.api.UserAuthenticationApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatWebSocketTicketService {

    private final SupportChatWebSocketTicketIssuer webSocketTicketIssuer;
    private final UserAuthenticationApi userAuthenticationApi;

    public String issue(CurrentUserSnapshot user) {
        String email = userAuthenticationApi
                .findUserAuthenticationById(user.id())
                .map(authentication -> authentication.email().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + user.id()));
        return webSocketTicketIssuer.issue(email);
    }
}
