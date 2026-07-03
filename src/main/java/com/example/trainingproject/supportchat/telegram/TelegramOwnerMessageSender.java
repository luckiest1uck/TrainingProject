package com.example.trainingproject.supportchat.telegram;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.trainingproject.supportchat.config.SupportChatProperties;
import com.example.trainingproject.supportchat.entity.SupportConversationEntity;
import com.example.trainingproject.supportchat.owner.OwnerMessage;
import com.example.trainingproject.supportchat.owner.OwnerMessageDeliveryResult;
import com.example.trainingproject.supportchat.owner.OwnerMessageSender;
import com.example.trainingproject.supportchat.repository.SupportConversationRepository;
import com.example.trainingproject.supportchat.repository.SupportMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "support-chat.owner-message-mode", havingValue = "TELEGRAM")
@RequiredArgsConstructor
class TelegramOwnerMessageSender implements OwnerMessageSender {

    private final SupportChatProperties properties;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final TelegramBotClient telegramBotClient;
    private final TelegramOwnerMessageFormatter formatter;

    @Override
    public OwnerMessageDeliveryResult send(OwnerMessage message) {
        UUID conversationId = message.conversationId();
        return conversationRepository
                .findById(conversationId)
                .map(conversation -> send(message, conversation))
                .orElseGet(() -> {
                    String logMessage = "support_chat.telegram.conversation_not_found: conversationId={}";
                    log.warn(logMessage, conversationId);
                    return OwnerMessageDeliveryResult.failedResult();
                });
    }

    private OwnerMessageDeliveryResult send(OwnerMessage message, SupportConversationEntity conversation) {
        String text = formatter.format(message);
        Long threadId = conversation.getTelegramMessageThreadId();
        UUID messageId = message.messageId();
        if (threadId != null) {
            OwnerMessageDeliveryResult topicDelivery = sendToExistingTopic(messageId, threadId, text);
            boolean delivered = topicDelivery.delivered();
            if (delivered) {
                return topicDelivery;
            }
            return sendFallback(messageId, conversation, text);
        }
        if (properties.telegram().forumTopicsEnabled()) {
            String topicName = formatter.topicName(message);
            Optional<TelegramForumTopic> topic = telegramBotClient.createForumTopic(topicName);
            if (topic.isPresent()) {
                long telegramMessageThreadId = topic.get().messageThreadId();
                conversation.setTelegramMessageThreadId(telegramMessageThreadId);
                conversationRepository.save(conversation);
                OwnerMessageDeliveryResult topicDelivery =
                        sendToExistingTopic(messageId, telegramMessageThreadId, text);
                if (!topicDelivery.delivered()) {
                    return sendFallback(messageId, conversation, text);
                }
                return topicDelivery;
            }
        }
        return sendFallback(messageId, conversation, text);
    }

    private OwnerMessageDeliveryResult sendToExistingTopic(UUID messageId, Long threadId, String text) {
        Optional<TelegramMessageRef> sent = telegramBotClient.sendMessage(threadId, text);
        if (sent.isPresent()) {
            TelegramMessageRef telegramMessageRef = sent.get();
            storeTelegramMessageId(messageId, telegramMessageRef.messageId());
            return OwnerMessageDeliveryResult.deliveredResult(telegramMessageRef.messageId());
        }
        return OwnerMessageDeliveryResult.failedResult();
    }

    private OwnerMessageDeliveryResult sendFallback(
            UUID messageId, SupportConversationEntity conversation, String text) {
        Optional<TelegramMessageRef> sent = telegramBotClient.sendMessage(null, text);
        if (sent.isEmpty()) {
            return OwnerMessageDeliveryResult.failedResult();
        }
        long telegramMessageId = sent.get().messageId();
        storeTelegramMessageId(messageId, telegramMessageId);
        conversation.setTelegramFallbackMessageId(telegramMessageId);
        conversationRepository.save(conversation);
        return OwnerMessageDeliveryResult.deliveredResult(telegramMessageId);
    }

    private void storeTelegramMessageId(UUID messageId, long telegramMessageId) {
        int updatedRows = messageRepository.updateTelegramMessageId(messageId, telegramMessageId);
        if (updatedRows != 1) {
            throw new IllegalStateException("Support chat telegram message correlation was not updated");
        }
    }
}
