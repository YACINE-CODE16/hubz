package com.hubz.domain.model;

import com.hubz.domain.enums.ChatbotIntent;
import com.hubz.domain.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a parsed chatbot message with extracted entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotParsedMessage {
    private ChatbotIntent intent;
    private String title;
    private String description;
    private LocalDate extractedDate;
    private LocalTime extractedTime;
    private TaskPriority priority;
    private String rawMessage;
    private Double confidence;
}
