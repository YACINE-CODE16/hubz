package com.hubz.application.service;

import com.hubz.application.dto.request.ResendVerificationRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.port.out.EmailVerificationTokenRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.EmailAlreadyVerifiedException;
import com.hubz.domain.exception.InvalidTokenException;
import com.hubz.domain.model.EmailVerificationToken;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepositoryPort tokenRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User unverifiedUser;
    private User verifiedUser;
    private EmailVerificationToken validToken;
    private EmailVerificationToken expiredToken;
    private EmailVerificationToken usedToken;

    @BeforeEach
    void setUp() {
        unverifiedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        verifiedUser = User.builder()
                .id(UUID.randomUUID())
                .email("verified@example.com")
                .password("hashedPassword")
                .firstName("Jane")
                .lastName("Doe")
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validToken = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(unverifiedUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        expiredToken = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(unverifiedUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now().minusHours(48))
                .expiresAt(LocalDateTime.now().minusHours(24))
                .used(false)
                .build();

        usedToken = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(unverifiedUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(true)
                .build();
    }

    @Test
    void createAndSendVerificationToken_shouldCreateTokenAndSendEmail() {
        // Given
        doNothing().when(tokenRepository).deleteByUserId(unverifiedUser.getId());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());

        // When
        emailVerificationService.createAndSendVerificationToken(unverifiedUser);

        // Then
        verify(tokenRepository).deleteByUserId(unverifiedUser.getId());
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmailVerificationEmail(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void verifyEmail_shouldVerifyUser_whenTokenIsValid() {
        // Given
        when(tokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));
        when(userRepository.findById(unverifiedUser.getId())).thenReturn(Optional.of(unverifiedUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        MessageResponse response = emailVerificationService.verifyEmail(validToken.getToken());

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("succès");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmailVerified()).isTrue();

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsed()).isTrue();
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("invalid-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenIsExpired() {
        // Given
        when(tokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(expiredToken.getToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenIsAlreadyUsed() {
        // Given
        when(tokenRepository.findByToken(usedToken.getToken())).thenReturn(Optional.of(usedToken));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(usedToken.getToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_shouldThrowException_whenEmailAlreadyVerified() {
        // Given
        EmailVerificationToken tokenForVerifiedUser = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(verifiedUser.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByToken(tokenForVerifiedUser.getToken())).thenReturn(Optional.of(tokenForVerifiedUser));
        when(userRepository.findById(verifiedUser.getId())).thenReturn(Optional.of(verifiedUser));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(tokenForVerifiedUser.getToken()))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void resendVerificationEmail_shouldResendEmail_whenUserExistsAndNotVerified() {
        // Given
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email(unverifiedUser.getEmail())
                .build();

        when(userRepository.findByEmail(unverifiedUser.getEmail())).thenReturn(Optional.of(unverifiedUser));
        doNothing().when(tokenRepository).deleteByUserId(unverifiedUser.getId());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());

        // When
        MessageResponse response = emailVerificationService.resendVerificationEmail(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_shouldReturnSuccess_whenUserDoesNotExist() {
        // Given
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        MessageResponse response = emailVerificationService.resendVerificationEmail(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Si un compte existe");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_shouldReturnSuccess_whenEmailAlreadyVerified() {
        // Given
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email(verifiedUser.getEmail())
                .build();

        when(userRepository.findByEmail(verifiedUser.getEmail())).thenReturn(Optional.of(verifiedUser));

        // When
        MessageResponse response = emailVerificationService.resendVerificationEmail(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Si un compte existe");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        // Given
        when(tokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));

        // When
        boolean result = emailVerificationService.isTokenValid(validToken.getToken());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        // Given
        when(tokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));

        // When
        boolean result = emailVerificationService.isTokenValid(expiredToken.getToken());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // When
        boolean result = emailVerificationService.isTokenValid("nonexistent");

        // Then
        assertThat(result).isFalse();
    }
}
