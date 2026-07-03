package com.example.trainingproject.review.service.ai.summary;

import java.util.UUID;

public interface ProductSummaryService {

    String summarize(UUID productId);
}
