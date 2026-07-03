package com.example.trainingproject.ratelimit.api;

import java.time.Duration;

public interface RateLimiter {

    RateLimitResult tryConsume(String key, int maxTokens, Duration windowDuration);
}
