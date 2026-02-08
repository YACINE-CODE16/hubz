package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for sending a message to the chatbot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "Le message ne peut pas etre vide")
    @Size(max = 1000, message = "Le message ne peut pas depasser 1000 caracteres")
    private String message;

    /**
     * Optional organization context. If provided, actions will be performed
     * within this organization context.
     */
    private UUID organizationId;
}
