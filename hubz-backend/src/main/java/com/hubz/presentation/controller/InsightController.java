package com.hubz.presentation.controller;

import com.hubz.application.dto.response.InsightResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.InsightService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for personal insights and recommendations.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get personalized insights and recommendations for the current user.
     *
     * @param authentication the current user's authentication
     * @return list of insights sorted by priority
     */
    @GetMapping("/insights")
    public ResponseEntity<List<InsightResponse>> getInsights(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        List<InsightResponse> insights = insightService.generateInsights(userId);
        return ResponseEntity.ok(insights);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
