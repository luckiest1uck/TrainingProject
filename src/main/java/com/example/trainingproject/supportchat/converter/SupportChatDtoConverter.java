package com.example.trainingproject.supportchat.converter;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.example.trainingproject.openapi.dto.SupportChatConversationDto;
import com.example.trainingproject.openapi.dto.SupportChatMessageDto;
import com.example.trainingproject.openapi.dto.SupportChatMessagePageDto;
import com.example.trainingproject.openapi.dto.SupportChatStatusDto;
import com.example.trainingproject.supportchat.entity.SupportConversationEntity;
import com.example.trainingproject.supportchat.entity.SupportMessageEntity;
import com.example.trainingproject.supportchat.service.SupportChatService.SupportChatStatus;

@Component
public class SupportChatDtoConverter {

    public SupportChatStatusDto toStatusDto(SupportChatStatus status) {
        SupportChatStatusDto dto = new SupportChatStatusDto();
        dto.setEnabled(status.enabled());
        dto.setEligible(status.eligible());
        if (status.reason() == null) {
            return dto;
        }
        dto.reason(SupportChatStatusDto.ReasonEnum.fromValue(status.reason()));
        return dto;
    }

    public SupportChatConversationDto toConversationDto(SupportConversationEntity conversation) {
        SupportChatConversationDto dto = new SupportChatConversationDto();
        dto.setId(conversation.getId());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setLastMessageAt(JsonNullable.of(conversation.getLastMessageAt()));
        return dto;
    }

    public SupportChatMessageDto toMessageDto(SupportMessageEntity message) {
        SupportChatMessageDto dto = new SupportChatMessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setClientMessageId(JsonNullable.of(message.getClientMessageId()));
        dto.setSenderType(SupportChatMessageDto.SenderTypeEnum.valueOf(
                message.getSenderType().name()));
        dto.setBody(message.getBody());
        dto.setDeliveryStatus(SupportChatMessageDto.DeliveryStatusEnum.valueOf(
                message.getDeliveryStatus().name()));
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    public SupportChatMessagePageDto toMessagePageDto(Page<SupportMessageEntity> messages) {
        SupportChatMessagePageDto dto = new SupportChatMessagePageDto();
        dto.setMessages(messages.stream().map(this::toMessageDto).toList());
        dto.setPage(messages.getNumber());
        dto.setSize(messages.getSize());
        dto.setTotalElements(messages.getTotalElements());
        dto.setTotalPages(messages.getTotalPages());
        return dto;
    }
}
