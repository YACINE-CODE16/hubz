package com.hubz.presentation.controller;

import com.hubz.application.dto.response.BackgroundJobResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.BackgroundJobService;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin controller for managing background jobs.
 * All endpoints require authentication; the authenticated user
 * acts as the admin (in a real system, role-based access would be enforced).
 */
@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
public class BackgroundJobController {

    private final BackgroundJobService backgroundJobService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * List all background jobs.
     */
    @GetMapping
    public ResponseEntity<List<BackgroundJobResponse>> getAllJobs(Authentication authentication) {
        resolveUserId(authentication);
        return ResponseEntity.ok(backgroundJobService.getAllJobs());
    }

    /**
     * Get a specific job by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BackgroundJobResponse> getJob(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        resolveUserId(authentication);
        return ResponseEntity.ok(backgroundJobService.getJob(id));
    }

    /**
     * Retry a failed job.
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<BackgroundJobResponse> retryJob(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        resolveUserId(authentication);
        return ResponseEntity.ok(backgroundJobService.retryJob(id));
    }

    /**
     * Retry all failed jobs.
     */
    @PostMapping("/retry-all")
    public ResponseEntity<Integer> retryAllFailedJobs(Authentication authentication) {
        resolveUserId(authentication);
        int count = backgroundJobService.retryFailedJobs();
        return ResponseEntity.ok(count);
    }

    /**
     * Trigger cleanup of old jobs (older than 30 days).
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Integer> cleanupOldJobs(Authentication authentication) {
        resolveUserId(authentication);
        int deleted = backgroundJobService.cleanupOldJobs();
        return ResponseEntity.ok(deleted);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return user.getId();
    }
}
