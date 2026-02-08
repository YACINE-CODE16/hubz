package com.hubz.application.port.out;

/**
 * Port interface for TOTP (Time-based One-Time Password) operations.
 * This follows the Clean Architecture pattern - application layer defines the interface,
 * infrastructure layer provides the implementation.
 */
public interface TotpServicePort {

    /**
     * Generates a new TOTP secret.
     *
     * @return the generated secret in Base32 format
     */
    String generateSecret();

    /**
     * Generates a QR code image for the TOTP secret.
     *
     * @param secret the TOTP secret
     * @param email  the user's email (used as account name in authenticator apps)
     * @param issuer the application name (e.g., "Hubz")
     * @return Base64-encoded PNG image of the QR code
     */
    String generateQrCodeImage(String secret, String email, String issuer);

    /**
     * Generates the otpauth URI for manual configuration.
     *
     * @param secret the TOTP secret
     * @param email  the user's email
     * @param issuer the application name
     * @return the otpauth URI
     */
    String generateOtpAuthUri(String secret, String email, String issuer);

    /**
     * Verifies a TOTP code against the secret.
     *
     * @param code   the 6-digit TOTP code to verify
     * @param secret the user's TOTP secret
     * @return true if the code is valid, false otherwise
     */
    boolean verifyCode(String code, String secret);
}
