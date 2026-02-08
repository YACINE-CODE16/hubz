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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TwoFactorAuthService Tests")
class TwoFactorAuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private TotpServicePort totpServicePort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TwoFactorAuthService twoFactorAuthService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .twoFactorEnabled(false)
                .twoFactorSecret(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(twoFactorAuthService, "appName", "Hubz");
    }

    @Nested
    @DisplayName("setup2FA Tests")
    class Setup2FATests {

        @Test
        @DisplayName("Should setup 2FA successfully for user without 2FA")
        void shouldSetup2FASuccessfully() {
            // Given
            String generatedSecret = "JBSWY3DPEHPK3PXP";
            String qrCodeImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgA...";
            String otpAuthUri = "otpauth://totp/Hubz:test@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Hubz";

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(totpServicePort.generateSecret()).thenReturn(generatedSecret);
            when(totpServicePort.generateQrCodeImage(eq(generatedSecret), eq("test@example.com"), eq("Hubz")))
                    .thenReturn(qrCodeImage);
            when(totpServicePort.generateOtpAuthUri(eq(generatedSecret), eq("test@example.com"), eq("Hubz")))
                    .thenReturn(otpAuthUri);
            when(userRepositoryPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TwoFactorSetupResponse response = twoFactorAuthService.setup2FA(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSecret()).isEqualTo(generatedSecret);
            assertThat(response.getQrCodeImage()).isEqualTo(qrCodeImage);
            assertThat(response.getOtpAuthUri()).isEqualTo(otpAuthUri);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepositoryPort).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getTwoFactorSecret()).isEqualTo(generatedSecret);
            assertThat(userCaptor.getValue().getTwoFactorEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.setup2FA(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when 2FA already enabled")
        void shouldThrowWhen2FAAlreadyEnabled() {
            // Given
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("existingSecret");
            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.setup2FA(userId))
                    .isInstanceOf(TwoFactorAuthException.class)
                    .hasMessageContaining("already enabled");
        }
    }

    @Nested
    @DisplayName("verify2FA Tests")
    class Verify2FATests {

        @Test
        @DisplayName("Should verify and enable 2FA successfully")
        void shouldVerifyAndEnable2FASuccessfully() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "123456";
            testUser.setTwoFactorSecret(secret);

            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code(code)
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(totpServicePort.verifyCode(code, secret)).thenReturn(true);
            when(userRepositoryPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TwoFactorStatusResponse response = twoFactorAuthService.verify2FA(userId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isTrue();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepositoryPort).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getTwoFactorEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when TOTP code is invalid")
        void shouldThrowWhenTotpCodeInvalid() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "000000";
            testUser.setTwoFactorSecret(secret);

            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code(code)
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(totpServicePort.verifyCode(code, secret)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.verify2FA(userId, request))
                    .isInstanceOf(InvalidTotpCodeException.class);
        }

        @Test
        @DisplayName("Should throw exception when 2FA setup not initiated")
        void shouldThrowWhenSetupNotInitiated() {
            // Given
            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code("123456")
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.verify2FA(userId, request))
                    .isInstanceOf(TwoFactorAuthException.class)
                    .hasMessageContaining("not been initiated");
        }

        @Test
        @DisplayName("Should throw exception when 2FA already enabled")
        void shouldThrowWhen2FAAlreadyEnabled() {
            // Given
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("existingSecret");

            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code("123456")
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.verify2FA(userId, request))
                    .isInstanceOf(TwoFactorAuthException.class)
                    .hasMessageContaining("already enabled");
        }
    }

    @Nested
    @DisplayName("disable2FA Tests")
    class Disable2FATests {

        @Test
        @DisplayName("Should disable 2FA successfully with valid password and code")
        void shouldDisable2FASuccessfully() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String password = "correctPassword";
            String code = "123456";
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret(secret);

            TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                    .password(password)
                    .code(code)
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(password, "hashedPassword")).thenReturn(true);
            when(totpServicePort.verifyCode(code, secret)).thenReturn(true);
            when(userRepositoryPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TwoFactorStatusResponse response = twoFactorAuthService.disable2FA(userId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isFalse();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepositoryPort).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getTwoFactorEnabled()).isFalse();
            assertThat(userCaptor.getValue().getTwoFactorSecret()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowWhenPasswordIncorrect() {
            // Given
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("secret");

            TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                    .password("wrongPassword")
                    .code("123456")
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.disable2FA(userId, request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepositoryPort, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when TOTP code is invalid")
        void shouldThrowWhenTotpCodeInvalid() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret(secret);

            TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                    .password("correctPassword")
                    .code("000000")
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);
            when(totpServicePort.verifyCode("000000", secret)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.disable2FA(userId, request))
                    .isInstanceOf(InvalidTotpCodeException.class);

            verify(userRepositoryPort, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when 2FA not enabled")
        void shouldThrowWhen2FANotEnabled() {
            // Given
            TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                    .password("password")
                    .code("123456")
                    .build();

            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.disable2FA(userId, request))
                    .isInstanceOf(TwoFactorAuthException.class)
                    .hasMessageContaining("not enabled");
        }
    }

    @Nested
    @DisplayName("get2FAStatus Tests")
    class Get2FAStatusTests {

        @Test
        @DisplayName("Should return enabled status when 2FA is enabled")
        void shouldReturnEnabledStatus() {
            // Given
            testUser.setTwoFactorEnabled(true);
            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TwoFactorStatusResponse response = twoFactorAuthService.get2FAStatus(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should return disabled status when 2FA is not enabled")
        void shouldReturnDisabledStatus() {
            // Given
            when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TwoFactorStatusResponse response = twoFactorAuthService.get2FAStatus(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> twoFactorAuthService.get2FAStatus(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("verifyLoginCode Tests")
    class VerifyLoginCodeTests {

        @Test
        @DisplayName("Should return true for valid TOTP code")
        void shouldReturnTrueForValidCode() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "123456";
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret(secret);

            when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(totpServicePort.verifyCode(code, secret)).thenReturn(true);

            // When
            boolean result = twoFactorAuthService.verifyLoginCode("test@example.com", code);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for invalid TOTP code")
        void shouldReturnFalseForInvalidCode() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "000000";
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret(secret);

            when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(totpServicePort.verifyCode(code, secret)).thenReturn(false);

            // When
            boolean result = twoFactorAuthService.verifyLoginCode("test@example.com", code);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when 2FA is not enabled")
        void shouldReturnTrueWhen2FANotEnabled() {
            // Given
            when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            boolean result = twoFactorAuthService.verifyLoginCode("test@example.com", "anycode");

            // Then
            assertThat(result).isTrue();
            verify(totpServicePort, never()).verifyCode(anyString(), anyString());
        }
    }
}
