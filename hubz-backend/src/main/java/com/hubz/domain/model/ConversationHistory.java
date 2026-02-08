package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the conversation history for contextual understanding.
 * Keeps track of recent messages for a user's session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {

    /**
     * Maximum number of messages to keep in history.
     */
    public static final int MAX_MESSAGES = 10;

    /**
     * User ID this history belongs to.
     */
    private UUID userId;

    /**
     * Organization context (optional).
     */
    private UUID organizationId;

    /**
     * List of messages in chronological order.
     */
    @Builder.Default
    private List<ConversationMessage> messages = new ArrayList<>();

    /**
     * Add a message to the history, maintaining the max limit.
     *
     * @param message The message to add
     */
    public void addMessage(ConversationMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        // Keep only the last MAX_MESSAGES
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    /**
     * Add a user message to the history.
     *
     * @param content The message content
     */
    public void addUserMessage(String content) {
        addMessage(ConversationMessage.user(content));
    }

    /**
     * Add an assistant message to the history.
     *
     * @param content The message content
     */
    public void addAssistantMessage(String content) {
        addMessage(ConversationMessage.assistant(content));
    }

    /**
     * Convert the history to a formatted string for LLM context.
     *
     * @return Formatted conversation history string
     */
    public String toContextString() {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Historique de conversation:\n");

        for (ConversationMessage msg : messages) {
            String roleLabel = "user".equals(msg.getRole()) ? "User" : "Assistant";
            sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Clear all messages from history.
     */
    public void clear() {
        if (messages != null) {
            messages.clear();
        }
    }

    /**
     * Get the number of messages in history.
     *
     * @return Message count
     */
    public int size() {
        return messages != null ? messages.size() : 0;
    }

    /**
     * Check if history is empty.
     *
     * @return true if no messages
     */
    public boolean isEmpty() {
        return messages == null || messages.isEmpty();
    }

    /**
     * Get the last user message content if present.
     *
     * @return Last user message or null
     */
    public String getLastUserMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }
}
