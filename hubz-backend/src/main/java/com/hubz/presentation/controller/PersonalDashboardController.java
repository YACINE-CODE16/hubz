package com.hubz.presentation.controller;

import com.hubz.application.dto.response.PersonalDashboardResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.PersonalDashboardService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PersonalDashboardController {

    private final PersonalDashboardService personalDashboardService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/users/me/dashboard")
    public ResponseEntity<PersonalDashboardResponse> getDashboard(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(personalDashboardService.getDashboard(userId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
