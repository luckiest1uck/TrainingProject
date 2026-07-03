package com.example.trainingproject.supportchat.service;

import org.springframework.stereotype.Service;

import com.example.trainingproject.security.api.dto.CurrentUserSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatWebSocketAccessService {

    private final SupportChatAvailabilityService availabilityService;
    private final SupportChatWebSocketTicketService webSocketTicketService;

    public String issueTicket(CurrentUserSnapshot user) {
        availabilityService.requireAvailable(user);
        return webSocketTicketService.issue(user);
    }
}
