package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTeamRequest;
import com.hubz.application.dto.request.UpdateTeamRequest;
import com.hubz.application.dto.response.TeamMemberResponse;
import com.hubz.application.dto.response.TeamResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TeamService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/teams")
    public ResponseEntity<List<TeamResponse>> getByOrganization(
            @PathVariable UUID orgId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/teams")
    public ResponseEntity<TeamResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.create(request, orgId, userId));
    }

    @PutMapping("/api/teams/{id}")
    public ResponseEntity<TeamResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/teams/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        teamService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/teams/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(
            @PathVariable UUID teamId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamService.getTeamMembers(teamId, currentUserId));
    }

    @PostMapping("/api/teams/{teamId}/members/{userId}")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        teamService.addMember(teamId, userId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/teams/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        teamService.removeMember(teamId, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
