package com.hubz.presentation.controller;

import com.hubz.application.dto.request.ForgotPasswordRequest;
import com.hubz.application.dto.request.LoginRequest;
import com.hubz.application.dto.request.RegisterRequest;
import com.hubz.application.dto.request.ResendVerificationRequest;
import com.hubz.application.dto.request.ResetPasswordRequest;
import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.service.AuthService;
import com.hubz.application.service.EmailVerificationService;
import com.hubz.application.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and account management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided email and password. Returns a JWT token upon successful registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates a user with email and password. Returns a JWT token for subsequent authenticated requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Get current user",
            description = "Returns the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    // Password reset endpoints

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the specified email address if it exists in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent if email exists",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request));
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using a valid reset token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @Operation(
            summary = "Validate reset token",
            description = "Checks if a password reset token is still valid."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired")
    })
    @GetMapping("/reset-password/{token}/valid")
    public ResponseEntity<MessageResponse> checkResetTokenValid(
            @Parameter(description = "Password reset token") @PathVariable String token) {
        boolean valid = passwordResetService.isTokenValid(token);
        if (valid) {
            return ResponseEntity.ok(MessageResponse.success("Token valide"));
        }
        return ResponseEntity.badRequest().body(MessageResponse.error("Token invalide ou expire"));
    }

    // Email verification endpoints

    @Operation(
            summary = "Verify email",
            description = "Verifies the user's email address using a verification token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify-email/{token}")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Parameter(description = "Email verification token") @PathVariable String token) {
        return ResponseEntity.ok(emailVerificationService.verifyEmail(token));
    }

    @Operation(
            summary = "Resend verification email",
            description = "Resends the email verification link to the specified email address."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification email sent",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email already verified or invalid")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(emailVerificationService.resendVerificationEmail(request));
    }

    @Operation(
            summary = "Validate verification token",
            description = "Checks if an email verification token is still valid."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired")
    })
    @GetMapping("/verify-email/{token}/valid")
    public ResponseEntity<MessageResponse> checkVerificationTokenValid(
            @Parameter(description = "Email verification token") @PathVariable String token) {
        boolean valid = emailVerificationService.isTokenValid(token);
        if (valid) {
            return ResponseEntity.ok(MessageResponse.success("Token valide"));
        }
        return ResponseEntity.badRequest().body(MessageResponse.error("Token invalide ou expire"));
    }
}
