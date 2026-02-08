package com.hubz.presentation.controller;

import com.hubz.application.dto.request.ChatMessageRequest;
import com.hubz.application.dto.response.ChatbotResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ChatbotService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for chatbot interactions.
 * Handles natural language message parsing and action execution.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Process a chatbot message and execute the corresponding action.
     *
     * @param request The chat message request containing the user's message
     * @param authentication The Spring Security authentication object
     * @return The chatbot response with parsed intent, entities, and action result
     */
    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> processMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        ChatbotResponse response = chatbotService.processMessage(request, userId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
