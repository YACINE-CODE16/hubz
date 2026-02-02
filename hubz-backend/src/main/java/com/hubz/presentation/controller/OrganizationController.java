package com.hubz.presentation.controller;

import com.hubz.application.dto.request.AddMemberRequest;
import com.hubz.application.dto.request.ChangeMemberRoleRequest;
import com.hubz.application.dto.request.CreateOrganizationRequest;
import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.response.MemberResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.OrganizationService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        UUID ownerId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request, ownerId));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAll() {
        return ResponseEntity.ok(organizationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.update(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.getMembers(id, currentUserId));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMember(id, request.getUserId(), request.getRole(), currentUserId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.removeMember(id, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.changeMemberRole(id, userId, request.getRole(), currentUserId));
    }

    @PostMapping("/{id}/transfer-ownership/{newOwnerId}")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable UUID id,
            @PathVariable UUID newOwnerId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.transferOwnership(id, newOwnerId, currentUserId);
        return ResponseEntity.ok().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
