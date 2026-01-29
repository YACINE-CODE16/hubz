package com.hubz.application.service;

import com.hubz.application.dto.request.LoginRequest;
import com.hubz.application.dto.request.RegisterRequest;
import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.JwtTokenPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidCredentialsException;
import com.hubz.domain.exception.UserAlreadyExistsException;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenPort jwtTokenPort;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .description("Test user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "Test user"
        );

        loginRequest = new LoginRequest(
                "test@example.com",
                "password123"
        );
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register a new user")
        void shouldRegisterNewUser() {
            // Given
            when(userRepositoryPort.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenPort.generateToken(testUser.getEmail())).thenReturn("jwt.token.here");

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getUser().getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(response.getUser().getLastName()).isEqualTo(testUser.getLastName());

            verify(userRepositoryPort).existsByEmail(registerRequest.getEmail());
            verify(passwordEncoder).encode(registerRequest.getPassword());
            verify(userRepositoryPort).save(any(User.class));
            verify(jwtTokenPort).generateToken(testUser.getEmail());
        }

        @Test
        @DisplayName("Should encode password before saving user")
        void shouldEncodePassword() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt.token.here");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            authService.register(registerRequest);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
            verify(passwordEncoder).encode(registerRequest.getPassword());
        }

        @Test
        @DisplayName("Should set createdAt and updatedAt timestamps")
        void shouldSetTimestamps() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt.token.here");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            authService.register(registerRequest);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(userRepositoryPort.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepositoryPort).existsByEmail(registerRequest.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepositoryPort, never()).save(any(User.class));
            verify(jwtTokenPort, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should generate UUID for new user")
        void shouldGenerateUUID() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenPort.generateToken(anyString())).thenReturn("jwt.token.here");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            authService.register(registerRequest);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginWithValidCredentials() {
            // Given
            when(userRepositoryPort.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
            when(jwtTokenPort.generateToken(testUser.getEmail())).thenReturn("jwt.token.here");

            // When
            AuthResponse response = authService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getUser().getFirstName()).isEqualTo(testUser.getFirstName());

            verify(userRepositoryPort).findByEmail(loginRequest.getEmail());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
            verify(jwtTokenPort).generateToken(testUser.getEmail());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepositoryPort.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepositoryPort).findByEmail(loginRequest.getEmail());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenPort, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            when(userRepositoryPort.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepositoryPort).findByEmail(loginRequest.getEmail());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
            verify(jwtTokenPort, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should not expose user data in response when login fails")
        void shouldNotExposeUserDataOnFailure() {
            // Given
            when(userRepositoryPort.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageNotContaining(loginRequest.getEmail());
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should successfully get current user by email")
        void shouldGetCurrentUser() {
            // Given
            String email = "test@example.com";
            when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When
            UserResponse response = authService.getCurrentUser(email);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(response.getLastName()).isEqualTo(testUser.getLastName());
            assertThat(response.getDescription()).isEqualTo(testUser.getDescription());
            assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());

            verify(userRepositoryPort).findByEmail(email);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String email = "nonexistent@example.com";
            when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.getCurrentUser(email))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(email);
        }

        @Test
        @DisplayName("Should not expose password in response")
        void shouldNotExposePassword() {
            // Given
            String email = "test@example.com";
            when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When
            UserResponse response = authService.getCurrentUser(email);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.toString()).doesNotContain("password");
        }
    }
}
