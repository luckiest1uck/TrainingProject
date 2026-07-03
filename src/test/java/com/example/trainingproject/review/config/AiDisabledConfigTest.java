package com.example.trainingproject.review.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.trainingproject.review.service.ai.moderation.ReviewModerationService;

@DisplayName("AiDisabledConfig")
class AiDisabledConfigTest {

    private final AiDisabledConfig config = new AiDisabledConfig();

    @Test
    @DisplayName("provides no-op moderation service")
    void noOpModerationService_doesNothing() {
        ReviewModerationService service = config.noOpModerationService();

        assertThatCode(() -> service.moderate("any review text")).doesNotThrowAnyException();
    }
}
