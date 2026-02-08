package com.hubz.application.service;

import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.port.out.GoogleOAuth2Port;
import com.hubz.application.port.out.JwtTokenPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.OAuth2AuthenticationException;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Service Unit Tests")
class OAuth2ServiceTest {

    @Mock
    private GoogleOAuth2Port googleOAuth2Port;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private JwtTokenPort jwtTokenPort;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OAuth2Service oAuth2Service;

    private Map<String, String> googleUserInfo;
    private User existingUser;

    @BeforeEach
    void setUp() {
        googleUserInfo = new HashMap<>();
        googleUserInfo.put("email", "john.doe@gmail.com");
        googleUserInfo.put("given_name", "John");
        googleUserInfo.put("family_name", "Doe");
        googleUserInfo.put("sub", "google-user-id-123");
        googleUserInfo.put("picture", "https://lh3.googleusercontent.com/photo.jpg");

        existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@gmail.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .twoFactorEnabled(false)
                .oauthProvider("google")
                .oauthProviderId("google-user-id-123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getGoogleAuthorizationUrl Tests")
    class GetGoogleAuthorizationUrlTests {

        @Test
        @DisplayName("Should return Google authorization URL from port")
        void shouldReturnGoogleAuthorizationUrl() {
            // Given
            String expectedUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test";
            when(googleOAuth2Port.buildAuthorizationUrl()).thenReturn(expectedUrl);

            // When
            String result = oAuth2Service.getGoogleAuthorizationUrl();

            // Then
            assertThat(result).isEqualTo(expectedUrl);
            verify(googleOAuth2Port).buildAuthorizationUrl();
        }
    }

    @Nested
    @DisplayName("handleGoogleCallback Tests - New User")
    class HandleGoogleCallbackNewUserTests {

        @Test
        @DisplayName("Should create new user when email does not exist")
        void shouldCreateNewUserWhenEmailNotFound() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code-123")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken("john.doe@gmail.com")).thenReturn("jwt-token-123");

            // When
            AuthResponse response = oAuth2Service.handleGoogleCallback("auth-code-123");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("john.doe@gmail.com");
            assertThat(response.getUser().getFirstName()).isEqualTo("John");
            assertThat(response.getUser().getLastName()).isEqualTo("Doe");
            assertThat(response.getUser().getOauthProvider()).isEqualTo("google");
            assertThat(response.isRequires2FA()).isFalse();

            verify(userRepositoryPort).save(any(User.class));
            verify(jwtTokenPort).generateToken("john.doe@gmail.com");
        }

        @Test
        @DisplayName("Should set email as verified for new OAuth user")
        void shouldSetEmailVerifiedForNewUser() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("Should set oauth provider and provider id for new user")
        void shouldSetOAuthProviderForNewUser() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getOauthProvider()).isEqualTo("google");
            assertThat(savedUser.getOauthProviderId()).isEqualTo("google-user-id-123");
        }

        @Test
        @DisplayName("Should set empty password for OAuth user")
        void shouldSetEmptyPasswordForOAuthUser() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEmpty();
        }

        @Test
        @DisplayName("Should set profile photo URL from Google")
        void shouldSetProfilePhotoUrl() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getProfilePhotoUrl()).isEqualTo("https://lh3.googleusercontent.com/photo.jpg");
        }

        @Test
        @DisplayName("Should send welcome email for new user")
        void shouldSendWelcomeEmailForNewUser() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(emailService).sendWelcomeEmail("john.doe@gmail.com", "John");
        }

        @Test
        @DisplayName("Should not fail if welcome email fails")
        void shouldNotFailIfWelcomeEmailFails() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");
            doThrow(new RuntimeException("SMTP error")).when(emailService).sendWelcomeEmail(anyString(), anyString());

            // When
            AuthResponse response = oAuth2Service.handleGoogleCallback("auth-code");

            // Then - should succeed despite email failure
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("Should use email prefix as firstName when given_name is null")
        void shouldUseEmailPrefixWhenFirstNameNull() {
            // Given
            googleUserInfo.put("given_name", null);
            googleUserInfo.put("family_name", null);
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFirstName()).isEqualTo("john.doe");
            assertThat(savedUser.getLastName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("handleGoogleCallback Tests - Existing User")
    class HandleGoogleCallbackExistingUserTests {

        @Test
        @DisplayName("Should login existing user with OAuth provider set")
        void shouldLoginExistingOAuthUser() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.of(existingUser));
            when(jwtTokenPort.generateToken("john.doe@gmail.com")).thenReturn("jwt-token-existing");

            // When
            AuthResponse response = oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-existing");
            assertThat(response.getUser().getEmail()).isEqualTo("john.doe@gmail.com");
            assertThat(response.isRequires2FA()).isFalse();

            // Should not create a new user
            verify(userRepositoryPort, never()).save(any(User.class));
            // Should not send welcome email
            verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Should link Google account to existing user without OAuth provider")
        void shouldLinkGoogleAccountToExistingUser() {
            // Given
            User userWithoutOAuth = User.builder()
                    .id(UUID.randomUUID())
                    .email("john.doe@gmail.com")
                    .password("encodedPassword")
                    .firstName("John")
                    .lastName("Doe")
                    .emailVerified(true)
                    .twoFactorEnabled(false)
                    .oauthProvider(null)
                    .oauthProviderId(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.of(userWithoutOAuth));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken("john.doe@gmail.com")).thenReturn("jwt-token-linked");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            AuthResponse response = oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-linked");

            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getOauthProvider()).isEqualTo("google");
            assertThat(savedUser.getOauthProviderId()).isEqualTo("google-user-id-123");
        }
    }

    @Nested
    @DisplayName("handleGoogleCallback Tests - Error Cases")
    class HandleGoogleCallbackErrorTests {

        @Test
        @DisplayName("Should throw exception when authorization code is null")
        void shouldThrowExceptionWhenCodeNull() {
            // When & Then
            assertThatThrownBy(() -> oAuth2Service.handleGoogleCallback(null))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessage("Authorization code is required");

            verify(googleOAuth2Port, never()).exchangeCodeForUserInfo(anyString());
        }

        @Test
        @DisplayName("Should throw exception when authorization code is blank")
        void shouldThrowExceptionWhenCodeBlank() {
            // When & Then
            assertThatThrownBy(() -> oAuth2Service.handleGoogleCallback("   "))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessage("Authorization code is required");

            verify(googleOAuth2Port, never()).exchangeCodeForUserInfo(anyString());
        }

        @Test
        @DisplayName("Should throw exception when Google API fails")
        void shouldThrowExceptionWhenGoogleApiFails() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("bad-code"))
                    .thenThrow(new RuntimeException("Invalid authorization code"));

            // When & Then
            assertThatThrownBy(() -> oAuth2Service.handleGoogleCallback("bad-code"))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("Failed to authenticate with Google");
        }

        @Test
        @DisplayName("Should throw exception when Google account has no email")
        void shouldThrowExceptionWhenNoEmail() {
            // Given
            googleUserInfo.put("email", null);
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);

            // When & Then
            assertThatThrownBy(() -> oAuth2Service.handleGoogleCallback("auth-code"))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessage("Google account does not have an email address");
        }

        @Test
        @DisplayName("Should throw exception when Google account has blank email")
        void shouldThrowExceptionWhenBlankEmail() {
            // Given
            googleUserInfo.put("email", "  ");
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);

            // When & Then
            assertThatThrownBy(() -> oAuth2Service.handleGoogleCallback("auth-code"))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessage("Google account does not have an email address");
        }
    }

    @Nested
    @DisplayName("handleGoogleCallback Tests - UUID and Timestamps")
    class HandleGoogleCallbackTimestampTests {

        @Test
        @DisplayName("Should generate UUID and timestamps for new user")
        void shouldGenerateUuidAndTimestamps() {
            // Given
            when(googleOAuth2Port.exchangeCodeForUserInfo("auth-code")).thenReturn(googleUserInfo);
            when(userRepositoryPort.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            oAuth2Service.handleGoogleCallback("auth-code");

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }
    }
}
