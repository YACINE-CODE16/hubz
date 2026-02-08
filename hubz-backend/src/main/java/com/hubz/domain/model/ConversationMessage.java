package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single message in a conversation history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    /**
     * Role of the message sender (user or assistant).
     */
    private String role;

    /**
     * Content of the message.
     */
    private String content;

    /**
     * Timestamp when the message was created.
     */
    private LocalDateTime timestamp;

    /**
     * Create a user message.
     */
    public static ConversationMessage user(String content) {
        return ConversationMessage.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an assistant message.
     */
    public static ConversationMessage assistant(String content) {
        return ConversationMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
