package com.example.trainingproject.review.service.ai.moderation;

import com.example.trainingproject.review.exception.ReviewModerationException;

public interface ReviewModerationService {

    /**
     * Validates review text against community guidelines.
     *
     * @throws ReviewModerationException if the content violates guidelines
     */
    void moderate(String text);
}
