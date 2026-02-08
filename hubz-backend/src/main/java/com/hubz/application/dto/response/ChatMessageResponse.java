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
public class ChatMessageResponse {
    private UUID id;
    private UUID teamId;
    private UUID userId;
    private String authorName;
    private String authorProfilePhotoUrl;
    private String content;
    private boolean deleted;
    private boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
}
