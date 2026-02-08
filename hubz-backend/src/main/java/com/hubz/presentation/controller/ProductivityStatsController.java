package com.hubz.presentation.controller;

import com.hubz.application.dto.response.ProductivityStatsResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ProductivityStatsService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for productivity statistics endpoints.
 * Provides personal productivity metrics for authenticated users.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class ProductivityStatsController {

    private final ProductivityStatsService productivityStatsService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get productivity statistics for the authenticated user.
     * Includes metrics like tasks completed, completion rates, streaks,
     * and period comparisons.
     *
     * @param authentication The authenticated user's details
     * @return ProductivityStatsResponse containing all productivity metrics
     */
    @GetMapping("/productivity-stats")
    public ResponseEntity<ProductivityStatsResponse> getProductivityStats(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        ProductivityStatsResponse stats = productivityStatsService.getProductivityStats(userId);
        return ResponseEntity.ok(stats);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
