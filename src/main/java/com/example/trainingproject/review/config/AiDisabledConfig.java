package com.example.trainingproject.review.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.trainingproject.review.service.ai.moderation.ReviewModerationService;
import com.example.trainingproject.review.service.ai.summary.ProductSummaryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "false", matchIfMissing = true)
class AiDisabledConfig {

    @Bean
    ReviewModerationService noOpModerationService() {
        return _ -> log.debug("ai.moderation.skipped: ai.enabled=false");
    }

    @Bean
    ProductSummaryService noOpProductSummaryService() {
        return _ -> null;
    }
}
