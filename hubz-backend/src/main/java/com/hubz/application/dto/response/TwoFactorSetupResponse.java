package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response containing the TOTP secret and QR code for 2FA setup.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {

    /**
     * The TOTP secret in Base32 format for manual entry in authenticator apps.
     */
    private String secret;

    /**
     * The QR code as a Base64-encoded PNG image for scanning with authenticator apps.
     */
    private String qrCodeImage;

    /**
     * The otpauth URI for manual configuration.
     */
    private String otpAuthUri;
}
