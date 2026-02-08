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
public class DirectMessageResponse {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private String senderProfilePhotoUrl;
    private UUID receiverId;
    private String receiverName;
    private String receiverProfilePhotoUrl;
    private String content;
    private boolean read;
    private boolean deleted;
    private boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
}
