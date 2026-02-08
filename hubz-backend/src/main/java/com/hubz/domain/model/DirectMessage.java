package com.hubz.domain.model;

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
public class DirectMessage {
    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private boolean read;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
}
