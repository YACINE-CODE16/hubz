package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID userId;
    private String userName;
    private String userProfilePhotoUrl;
    private String lastMessageContent;
    private UUID lastMessageSenderId;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
}
