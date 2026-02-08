package com.hubz.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.dto.request.TwoFactorDisableRequest;
import com.hubz.application.dto.request.TwoFactorVerifyRequest;
import com.hubz.application.dto.response.TwoFactorSetupResponse;
import com.hubz.application.dto.response.TwoFactorStatusResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TwoFactorAuthService;
import com.hubz.domain.exception.InvalidTotpCodeException;
import com.hubz.domain.exception.TwoFactorAuthException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TwoFactorAuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("TwoFactorAuthController Tests")
class TwoFactorAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TwoFactorAuthService twoFactorAuthService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private User testUser;
    private Authentication mockAuth;

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/setup")
    class Setup2FATests {

        @Test
        @DisplayName("Should return setup response with secret and QR code")
        void shouldReturnSetupResponse() throws Exception {
            // Given
            TwoFactorSetupResponse response = TwoFactorSetupResponse.builder()
                    .secret("JBSWY3DPEHPK3PXP")
                    .qrCodeImage("data:image/png;base64,...")
                    .otpAuthUri("otpauth://totp/Hubz:test@example.com?secret=JBSWY3DPEHPK3PXP")
                    .build();

            when(twoFactorAuthService.setup2FA(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/2fa/setup")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"))
                    .andExpect(jsonPath("$.qrCodeImage").value("data:image/png;base64,..."))
                    .andExpect(jsonPath("$.otpAuthUri").exists());
        }

        @Test
        @DisplayName("Should return 400 when 2FA already enabled")
        void shouldReturn400When2FAAlreadyEnabled() throws Exception {
            // Given
            when(twoFactorAuthService.setup2FA(userId))
                    .thenThrow(new TwoFactorAuthException("2FA is already enabled"));

            // When & Then
            mockMvc.perform(post("/api/auth/2fa/setup")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/2fa/verify")
    class Verify2FATests {

        @Test
        @DisplayName("Should verify and enable 2FA successfully")
        void shouldVerifyAndEnable2FA() throws Exception {
            // Given
            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code("123456")
                    .build();

            TwoFactorStatusResponse response = TwoFactorStatusResponse.builder()
                    .enabled(true)
                    .message("2FA enabled successfully")
                    .build();

            when(twoFactorAuthService.verify2FA(eq(userId), any(TwoFactorVerifyRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("Should return 401 when TOTP code is invalid")
        void shouldReturn401WhenCodeInvalid() throws Exception {
            // Given
            TwoFactorVerifyRequest request = TwoFactorVerifyRequest.builder()
                    .code("000000")
                    .build();

            when(twoFactorAuthService.verify2FA(eq(userId), any(TwoFactorVerifyRequest.class)))
                    .thenThrow(new InvalidTotpCodeException("Invalid code"));

            // When & Then
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when code is missing")
        void shouldReturn400WhenCodeMissing() throws Exception {
            // Given - empty request without code
            String invalidRequest = "{}";

            // When & Then
            mockMvc.perform(post("/api/auth/2fa/verify")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/2fa/disable")
    class Disable2FATests {

        @Test
        @DisplayName("Should disable 2FA successfully")
        void shouldDisable2FA() throws Exception {
            // Given
            TwoFactorDisableRequest request = TwoFactorDisableRequest.builder()
                    .password("password123")
                    .code("123456")
                    .build();

            TwoFactorStatusResponse response = TwoFactorStatusResponse.builder()
                    .enabled(false)
                    .message("2FA disabled successfully")
                    .build();

            when(twoFactorAuthService.disable2FA(eq(userId), any(TwoFactorDisableRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(delete("/api/auth/2fa/disable")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Should return 400 when password missing")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Given
            String invalidRequest = "{\"code\": \"123456\"}";

            // When & Then
            mockMvc.perform(delete("/api/auth/2fa/disable")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/2fa/status")
    class Get2FAStatusTests {

        @Test
        @DisplayName("Should return 2FA status")
        void shouldReturn2FAStatus() throws Exception {
            // Given
            TwoFactorStatusResponse response = TwoFactorStatusResponse.builder()
                    .enabled(false)
                    .message("2FA is not enabled")
                    .build();

            when(twoFactorAuthService.get2FAStatus(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/auth/2fa/status")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }
}
