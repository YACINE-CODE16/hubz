package com.hubz.application.service;

import com.hubz.application.dto.request.ForgotPasswordRequest;
import com.hubz.application.dto.request.ResetPasswordRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.port.out.PasswordResetTokenRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidTokenException;
import com.hubz.domain.model.PasswordResetToken;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepositoryPort tokenRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;
    private PasswordResetToken usedToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        expiredToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        usedToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();
    }

    @Test
    void requestPasswordReset_shouldCreateTokenAndSendEmail_whenUserExists() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(testUser.getEmail())
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        doNothing().when(tokenRepository).deleteByUserId(testUser.getId());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        // When
        MessageResponse response = passwordResetService.requestPasswordReset(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Si un compte existe");

        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_shouldReturnSuccess_whenUserDoesNotExist() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        MessageResponse response = passwordResetService.requestPasswordReset(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Si un compte existe");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(validToken.getToken())
                .newPassword("newPassword123")
                .build();

        when(tokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        MessageResponse response = passwordResetService.resetPassword(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("succès");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("newHashedPassword");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsed()).isTrue();
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenNotFound() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("newPassword123")
                .build();

        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsExpired() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(expiredToken.getToken())
                .newPassword("newPassword123")
                .build();

        when(tokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsAlreadyUsed() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(usedToken.getToken())
                .newPassword("newPassword123")
                .build();

        when(tokenRepository.findByToken(usedToken.getToken())).thenReturn(Optional.of(usedToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        // Given
        when(tokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));

        // When
        boolean result = passwordResetService.isTokenValid(validToken.getToken());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        // Given
        when(tokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));

        // When
        boolean result = passwordResetService.isTokenValid(expiredToken.getToken());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // When
        boolean result = passwordResetService.isTokenValid("nonexistent");

        // Then
        assertThat(result).isFalse();
    }
}
