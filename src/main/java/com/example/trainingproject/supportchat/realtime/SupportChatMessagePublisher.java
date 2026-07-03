package com.example.trainingproject.supportchat.realtime;

import com.example.trainingproject.supportchat.entity.SupportConversationEntity;
import com.example.trainingproject.supportchat.entity.SupportMessageEntity;

public interface SupportChatMessagePublisher {

    void publishOwnerReply(SupportConversationEntity conversation, SupportMessageEntity message);
}
