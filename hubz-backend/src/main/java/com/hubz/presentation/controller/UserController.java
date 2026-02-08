package com.hubz.presentation.controller;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.DeleteAccountRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and settings management")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Update profile",
            description = "Updates the current user's profile information (first name, last name, description)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Change password",
            description = "Changes the current user's password. Requires the current password for verification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid current password or weak new password",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Upload profile photo",
            description = "Uploads or updates the user's profile photo. Max file size: 5MB. Allowed types: jpg, jpeg, png, gif, webp."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/photo")
    public ResponseEntity<UserResponse> uploadProfilePhoto(
            Principal principal,
            @Parameter(description = "Profile photo file") @RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadProfilePhoto(principal.getName(), file);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete profile photo",
            description = "Removes the user's profile photo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo deleted successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/photo")
    public ResponseEntity<UserResponse> deleteProfilePhoto(Principal principal) {
        UserResponse response = userService.deleteProfilePhoto(principal.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete account",
            description = """
                    Permanently deletes the user's account. Requires password confirmation.

                    This action will:
                    - Remove the user from all organizations
                    - Transfer ownership of owned organizations to another admin (if available)
                    - Delete organizations with no other members
                    - Permanently delete all user data

                    **This action cannot be undone.**
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or unable to delete",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            Principal principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
