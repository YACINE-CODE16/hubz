package com.hubz.infrastructure.security;

import com.hubz.application.port.out.TotpServicePort;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Implementation of TOTP service using the dev.samstevens.totp library.
 */
@Service
@Slf4j
public class TotpService implements TotpServicePort {

    private static final int SECRET_LENGTH = 32;
    private static final int TIME_PERIOD = 30; // seconds
    private static final int CODE_DIGITS = 6;
    private static final int ALLOWED_TIME_DISCREPANCY = 1; // Allow 1 period before/after

    private final SecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator;

    @Value("${app.name:Hubz}")
    private String appName;

    public TotpService() {
        this.secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
        this.codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS);

        TimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        ((DefaultCodeVerifier) this.codeVerifier).setTimePeriod(TIME_PERIOD);
        ((DefaultCodeVerifier) this.codeVerifier).setAllowedTimePeriodDiscrepancy(ALLOWED_TIME_DISCREPANCY);

        this.qrGenerator = new ZxingPngQrGenerator();
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String generateQrCodeImage(String secret, String email, String issuer) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer != null ? issuer : appName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(CODE_DIGITS)
                .period(TIME_PERIOD)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(qrData);
            // Return as data URI for direct embedding in HTML/React
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code for user: {}", email, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public String generateOtpAuthUri(String secret, String email, String issuer) {
        String effectiveIssuer = issuer != null ? issuer : appName;
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                effectiveIssuer,
                email,
                secret,
                effectiveIssuer,
                CODE_DIGITS,
                TIME_PERIOD
        );
    }

    @Override
    public boolean verifyCode(String code, String secret) {
        if (code == null || secret == null) {
            return false;
        }

        // Remove any spaces or dashes the user might have entered
        String cleanCode = code.replaceAll("[\\s-]", "");

        if (cleanCode.length() != CODE_DIGITS) {
            return false;
        }

        try {
            return codeVerifier.isValidCode(secret, cleanCode);
        } catch (Exception e) {
            log.warn("Error verifying TOTP code", e);
            return false;
        }
    }
}
