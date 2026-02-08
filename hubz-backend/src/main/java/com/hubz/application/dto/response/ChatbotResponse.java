package com.hubz.application.dto.response;

import com.hubz.domain.enums.ChatbotIntent;
import com.hubz.domain.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO from the chatbot containing parsed intent, entities, and action results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {

    /**
     * The detected intent from the user's message.
     */
    private ChatbotIntent intent;

    /**
     * Extracted entities from the message.
     */
    private ExtractedEntities entities;

    /**
     * Human-readable confirmation text.
     */
    private String confirmationText;

    /**
     * URL to navigate to after the action (optional).
     */
    private String actionUrl;

    /**
     * Whether the action was executed successfully.
     */
    private boolean actionExecuted;

    /**
     * Error message if the action failed.
     */
    private String errorMessage;

    /**
     * ID of the created resource (task, event, goal, note).
     */
    private UUID createdResourceId;

    /**
     * Quick action buttons for the user.
     */
    private List<QuickAction> quickActions;

    /**
     * Query results when the intent is a query type.
     */
    private QueryResults queryResults;

    /**
     * Whether Ollama LLM was used for processing (vs regex fallback).
     */
    private boolean usedOllama;

    /**
     * The Ollama model name used (if Ollama was used).
     */
    private String ollamaModel;

    /**
     * Nested class for extracted entities.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedEntities {
        private String title;
        private String description;
        private LocalDate date;
        private LocalTime time;
        private TaskPriority priority;
        private Map<String, String> additionalEntities;
    }

    /**
     * Quick action button representation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String label;
        private String action;
        private String url;
    }

    /**
     * Query results for QUERY_* intents.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryResults {
        private int totalCount;
        private List<Map<String, Object>> items;
        private String summary;
    }
}
