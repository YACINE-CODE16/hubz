package com.hubz.application.service;

import com.hubz.application.dto.request.TwoFactorDisableRequest;
import com.hubz.application.dto.request.TwoFactorVerifyRequest;
import com.hubz.application.dto.response.TwoFactorSetupResponse;
import com.hubz.application.dto.response.TwoFactorStatusResponse;
import com.hubz.application.port.out.TotpServicePort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidCredentialsException;
import com.hubz.domain.exception.InvalidTotpCodeException;
import com.hubz.domain.exception.TwoFactorAuthException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing Two-Factor Authentication (2FA) operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final TotpServicePort totpServicePort;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.name:Hubz}")
    private String appName;

    /**
     * Initiates 2FA setup for a user by generating a secret and QR code.
     * The secret is temporarily stored but 2FA is not enabled until verified.
     *
     * @param userId the ID of the user setting up 2FA
     * @return setup response containing secret and QR code
     */
    @Transactional
    public TwoFactorSetupResponse setup2FA(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.isTwoFactorEnabled()) {
            throw new TwoFactorAuthException("2FA is already enabled for this account");
        }

        // Generate a new secret
        String secret = totpServicePort.generateSecret();

        // Store the secret (but don't enable 2FA yet)
        user.setTwoFactorSecret(secret);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);

        // Generate QR code and URI
        String qrCodeImage = totpServicePort.generateQrCodeImage(secret, user.getEmail(), appName);
        String otpAuthUri = totpServicePort.generateOtpAuthUri(secret, user.getEmail(), appName);

        log.info("2FA setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeImage(qrCodeImage)
                .otpAuthUri(otpAuthUri)
                .build();
    }

    /**
     * Verifies the TOTP code and enables 2FA for the user.
     *
     * @param userId  the ID of the user
     * @param request the verification request containing the TOTP code
     * @return status response indicating success
     */
    @Transactional
    public TwoFactorStatusResponse verify2FA(UUID userId, TwoFactorVerifyRequest request) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.isTwoFactorEnabled()) {
            throw new TwoFactorAuthException("2FA is already enabled for this account");
        }

        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isEmpty()) {
            throw new TwoFactorAuthException("2FA setup has not been initiated. Please call setup first.");
        }

        // Verify the TOTP code
        if (!totpServicePort.verifyCode(request.getCode(), secret)) {
            throw new InvalidTotpCodeException("Invalid TOTP code. Please try again.");
        }

        // Enable 2FA
        user.setTwoFactorEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);

        log.info("2FA enabled for user: {}", user.getEmail());

        return TwoFactorStatusResponse.builder()
                .enabled(true)
                .message("L'authentification à deux facteurs a été activée avec succès")
                .build();
    }

    /**
     * Disables 2FA for a user. Requires password and current TOTP code for security.
     *
     * @param userId  the ID of the user
     * @param request the disable request containing password and TOTP code
     * @return status response indicating success
     */
    @Transactional
    public TwoFactorStatusResponse disable2FA(UUID userId, TwoFactorDisableRequest request) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (!user.isTwoFactorEnabled()) {
            throw new TwoFactorAuthException("2FA is not enabled for this account");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Mot de passe incorrect");
        }

        // Verify TOTP code
        if (!totpServicePort.verifyCode(request.getCode(), user.getTwoFactorSecret())) {
            throw new InvalidTotpCodeException("Code TOTP invalide");
        }

        // Disable 2FA
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);

        log.info("2FA disabled for user: {}", user.getEmail());

        return TwoFactorStatusResponse.builder()
                .enabled(false)
                .message("L'authentification à deux facteurs a été désactivée")
                .build();
    }

    /**
     * Gets the current 2FA status for a user.
     *
     * @param userId the ID of the user
     * @return status response
     */
    public TwoFactorStatusResponse get2FAStatus(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        return TwoFactorStatusResponse.builder()
                .enabled(user.isTwoFactorEnabled())
                .message(user.isTwoFactorEnabled()
                        ? "L'authentification à deux facteurs est activée"
                        : "L'authentification à deux facteurs n'est pas activée")
                .build();
    }

    /**
     * Verifies a TOTP code for a user during login.
     *
     * @param email the user's email
     * @param code  the TOTP code to verify
     * @return true if the code is valid
     */
    public boolean verifyLoginCode(String email, String code) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!user.isTwoFactorEnabled()) {
            return true; // 2FA not enabled, no verification needed
        }

        return totpServicePort.verifyCode(code, user.getTwoFactorSecret());
    }
}
