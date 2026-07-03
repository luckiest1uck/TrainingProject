package com.example.trainingproject.supportchat.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trainingproject.security.api.dto.CurrentUserSnapshot;
import com.example.trainingproject.supportchat.config.SupportChatProperties;
import com.example.trainingproject.supportchat.exception.SupportChatAccessRestrictedException;
import com.example.trainingproject.supportchat.exception.SupportChatDisabledException;
import com.example.trainingproject.supportchat.exception.SupportChatEmailVerificationRequiredException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatAvailabilityService {

    private final SupportChatProperties properties;
    private final SupportChatEligibilityService eligibilityService;

    public void requireAvailable(CurrentUserSnapshot user) {
        requireAvailable(user.id());
    }

    public void requireAvailable(UUID userId) {
        if (!properties.enabled()) {
            throw new SupportChatDisabledException();
        }

        SupportChatEligibility eligibility = eligibilityService.eligibilityFor(userId);
        if (!eligibility.eligible()) {
            if (eligibility.isAccessRestricted()) {
                throw new SupportChatAccessRestrictedException();
            }
            throw new SupportChatEmailVerificationRequiredException();
        }
    }

    public boolean isEligible(UUID userId) {
        return properties.enabled() && eligibilityService.eligibilityFor(userId).eligible();
    }
}
