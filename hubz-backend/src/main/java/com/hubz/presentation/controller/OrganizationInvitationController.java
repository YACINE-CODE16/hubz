package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateInvitationRequest;
import com.hubz.application.dto.response.InvitationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.OrganizationInvitationService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationInvitationController {

    private final OrganizationInvitationService invitationService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/organizations/{orgId}/invitations")
    public ResponseEntity<InvitationResponse> createInvitation(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(orgId, request, userId));
    }

    @GetMapping("/organizations/{orgId}/invitations")
    public ResponseEntity<List<InvitationResponse>> getInvitations(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(invitationService.getInvitations(orgId, userId));
    }

    @GetMapping("/invitations/{token}/info")
    public ResponseEntity<Map<String, Object>> getInvitationInfo(@PathVariable String token) {
        try {
            InvitationResponse invitation = invitationService.getInvitationByToken(token);
            return ResponseEntity.ok(Map.of(
                    "organizationId", invitation.getOrganizationId(),
                    "email", invitation.getEmail(),
                    "role", invitation.getRole(),
                    "expiresAt", invitation.getExpiresAt(),
                    "used", invitation.getUsed()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @PathVariable String token,
            Authentication authentication) {
        try {
            UUID userId = resolveUserId(authentication);
            invitationService.acceptInvitation(token, userId);
            return ResponseEntity.ok(Map.of("message", "Invitation accepted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        invitationService.deleteInvitation(invitationId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
