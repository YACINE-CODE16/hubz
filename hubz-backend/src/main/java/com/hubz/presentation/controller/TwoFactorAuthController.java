package com.hubz.presentation.controller;

import com.hubz.application.dto.request.TwoFactorDisableRequest;
import com.hubz.application.dto.request.TwoFactorVerifyRequest;
import com.hubz.application.dto.response.TwoFactorSetupResponse;
import com.hubz.application.dto.response.TwoFactorStatusResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TwoFactorAuthService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Two-Factor Authentication (2FA) operations.
 */
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Initiates 2FA setup by generating a new secret and QR code.
     * The user must verify the code before 2FA is actually enabled.
     *
     * @param authentication the current authentication
     * @return setup response with secret and QR code
     */
    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2FA(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        TwoFactorSetupResponse response = twoFactorAuthService.setup2FA(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the TOTP code and enables 2FA for the user.
     *
     * @param authentication the current authentication
     * @param request        the verification request with TOTP code
     * @return status response
     */
    @PostMapping("/verify")
    public ResponseEntity<TwoFactorStatusResponse> verify2FA(
            Authentication authentication,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        UUID userId = resolveUserId(authentication);
        TwoFactorStatusResponse response = twoFactorAuthService.verify2FA(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Disables 2FA for the user. Requires password and current TOTP code.
     *
     * @param authentication the current authentication
     * @param request        the disable request with password and TOTP code
     * @return status response
     */
    @DeleteMapping("/disable")
    public ResponseEntity<TwoFactorStatusResponse> disable2FA(
            Authentication authentication,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        UUID userId = resolveUserId(authentication);
        TwoFactorStatusResponse response = twoFactorAuthService.disable2FA(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the current 2FA status for the authenticated user.
     *
     * @param authentication the current authentication
     * @return status response
     */
    @GetMapping("/status")
    public ResponseEntity<TwoFactorStatusResponse> get2FAStatus(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        TwoFactorStatusResponse response = twoFactorAuthService.get2FAStatus(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resolves the user ID from the authentication.
     */
    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
