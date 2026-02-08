package com.hubz.presentation.controller;

import com.hubz.application.dto.request.AddMemberRequest;
import com.hubz.application.dto.request.ChangeMemberRoleRequest;
import com.hubz.application.dto.request.CreateOrganizationRequest;
import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.response.MemberResponse;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.OrganizationService;
import com.hubz.domain.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization CRUD and member management")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepositoryPort userRepositoryPort;

    @Operation(
            summary = "Create organization",
            description = "Creates a new organization. The authenticated user becomes the owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization created successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        UUID ownerId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request, ownerId));
    }

    @Operation(
            summary = "Get all organizations",
            description = "Returns all organizations the authenticated user is a member of."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAll() {
        return ResponseEntity.ok(organizationService.getAll());
    }

    @Operation(
            summary = "Get organization by ID",
            description = "Returns the details of a specific organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @Operation(
            summary = "Update organization",
            description = "Updates an organization's details. Requires ADMIN or OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization updated successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or OWNER role"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.update(id, request, currentUserId));
    }

    @Operation(
            summary = "Delete organization",
            description = "Deletes an organization permanently. Only the owner can delete the organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organization deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - only OWNER can delete"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get organization members",
            description = "Returns all members of an organization with their roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.getMembers(id, currentUserId));
    }

    @Operation(
            summary = "Add member to organization",
            description = "Adds a new member to the organization. Requires ADMIN or OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added successfully",
                    content = @Content(schema = @Schema(implementation = MemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already a member"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or OWNER role"),
            @ApiResponse(responseCode = "404", description = "Organization or user not found")
    })
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMember(id, request.getUserId(), request.getRole(), currentUserId));
    }

    @Operation(
            summary = "Remove member from organization",
            description = "Removes a member from the organization. Requires ADMIN or OWNER role. Owner cannot be removed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot remove owner"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or OWNER role"),
            @ApiResponse(responseCode = "404", description = "Organization or member not found")
    })
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Parameter(description = "User ID to remove") @PathVariable UUID userId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.removeMember(id, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change member role",
            description = "Changes a member's role in the organization. Requires OWNER role to change other members' roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role changed successfully",
                    content = @Content(schema = @Schema(implementation = MemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role or cannot change owner's role"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - only OWNER can change roles"),
            @ApiResponse(responseCode = "404", description = "Organization or member not found")
    })
    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Parameter(description = "User ID to update") @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(organizationService.changeMemberRole(id, userId, request.getRole(), currentUserId));
    }

    @Operation(
            summary = "Transfer ownership",
            description = "Transfers organization ownership to another member. Only the current owner can perform this action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ownership transferred successfully"),
            @ApiResponse(responseCode = "400", description = "Target user is not a member"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - only OWNER can transfer ownership"),
            @ApiResponse(responseCode = "404", description = "Organization or user not found")
    })
    @PostMapping("/{id}/transfer-ownership/{newOwnerId}")
    public ResponseEntity<Void> transferOwnership(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Parameter(description = "New owner's user ID") @PathVariable UUID newOwnerId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        organizationService.transferOwnership(id, newOwnerId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Upload organization logo",
            description = "Uploads or updates the organization's logo. Requires ADMIN or OWNER role. Max file size: 5MB. Allowed types: jpg, jpeg, png, gif, webp."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or OWNER role"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PostMapping("/{id}/logo")
    public ResponseEntity<OrganizationResponse> uploadLogo(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Parameter(description = "Logo image file") @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        OrganizationResponse response = organizationService.uploadLogo(id, file, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete organization logo",
            description = "Removes the organization's logo. Requires ADMIN or OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo deleted successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN or OWNER role"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @DeleteMapping("/{id}/logo")
    public ResponseEntity<OrganizationResponse> deleteLogo(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        OrganizationResponse response = organizationService.deleteLogo(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
