package com.example.trainingproject.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.trainingproject.common.monitoring.SentryJobMonitor;
import com.example.trainingproject.supportchat.config.SupportChatProperties;
import com.example.trainingproject.supportchat.config.SupportChatProperties.Bucket;
import com.example.trainingproject.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.example.trainingproject.supportchat.config.SupportChatProperties.RateLimits;
import com.example.trainingproject.supportchat.config.SupportChatProperties.Telegram;
import com.example.trainingproject.supportchat.config.SupportChatProperties.Turnstile;
import com.example.trainingproject.supportchat.repository.SupportMessageRepository;

@DisplayName("SupportChatRetentionCleanupJob unit tests")
class SupportChatRetentionCleanupJobTest {

    private final SupportMessageRepository messageRepository = mock(SupportMessageRepository.class);
    private final SentryJobMonitor sentryJobMonitor = mock(SentryJobMonitor.class);

    @Test
    @DisplayName("Deletes messages older than configured retention window")
    void cleanupExpiredMessagesInternal_deletesExpiredMessages() {
        when(messageRepository.deleteByCreatedAtBefore(any())).thenReturn(3L);
        SupportChatRetentionCleanupJob job =
                new SupportChatRetentionCleanupJob(properties(), messageRepository, sentryJobMonitor);

        long deletedCount = job.cleanupExpiredMessagesInternal();

        assertThat(deletedCount).isEqualTo(3L);
        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(messageRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBefore(OffsetDateTime.now().minusDays(89));
        assertThat(cutoffCaptor.getValue()).isAfter(OffsetDateTime.now().minusDays(91));
    }

    @Test
    @DisplayName("Scheduled cleanup runs through Sentry monitor with the configured interval")
    void cleanupExpiredMessages_runsThroughSentryMonitor() {
        when(messageRepository.deleteByCreatedAtBefore(any())).thenReturn(1L);
        doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(2)).run();
                    return null;
                })
                .when(sentryJobMonitor)
                .run(eq("support-chat-retention-cleanup"), any(), any(Runnable.class));
        SupportChatRetentionCleanupJob job =
                new SupportChatRetentionCleanupJob(properties(), messageRepository, sentryJobMonitor);
        org.springframework.test.util.ReflectionTestUtils.setField(
                job, "retentionCleanupInterval", Duration.ofHours(24));

        job.cleanupExpiredMessages();

        verify(sentryJobMonitor).run(eq("support-chat-retention-cleanup"), any(), any(Runnable.class));
        verify(messageRepository).deleteByCreatedAtBefore(any());
    }

    private static SupportChatProperties properties() {
        return new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.FAKE,
                new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5)),
                new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5)),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1))),
                Set.of());
    }
}
