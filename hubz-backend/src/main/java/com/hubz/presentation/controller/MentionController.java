package com.hubz.presentation.controller;

import com.hubz.application.dto.response.MentionableUserResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.AuthorizationService;
import com.hubz.application.service.MentionService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for mention-related endpoints.
 * Provides the mentionable users list for the autocomplete feature.
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/mentions")
@RequiredArgsConstructor
public class MentionController {

    private final MentionService mentionService;
    private final AuthorizationService authorizationService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get all mentionable users in an organization.
     * Used for the @mention autocomplete in the frontend.
     *
     * @param organizationId the organization ID
     * @param authentication the current user's authentication
     * @return list of mentionable users
     */
    @GetMapping("/users")
    public ResponseEntity<List<MentionableUserResponse>> getMentionableUsers(
            @PathVariable UUID organizationId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        // Check if user has access to the organization
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<MentionableUserResponse> users = mentionService.getMentionableUsers(organizationId)
                .stream()
                .map(user -> MentionableUserResponse.builder()
                        .userId(user.getUserId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .displayName(user.getDisplayName())
                        .mentionName(user.getMentionName())
                        .profilePhotoUrl(user.getProfilePhotoUrl())
                        .build())
                .toList();

        return ResponseEntity.ok(users);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
