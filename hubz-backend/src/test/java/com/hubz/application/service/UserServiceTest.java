package com.hubz.application.service;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidPasswordException;
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
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email(userEmail)
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .description("Original description")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should successfully update user profile")
        void shouldUpdateProfile() {
            // Given
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .description("Updated description")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateProfile(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Smith");
            assertThat(response.getDescription()).isEqualTo("Updated description");
            assertThat(response.getEmail()).isEqualTo(userEmail);
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());

            verify(userRepositoryPort).findByEmail(userEmail);
            verify(userRepositoryPort).save(any(User.class));
        }

        @Test
        @DisplayName("Should update the updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.updateProfile(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should allow null description")
        void shouldAllowNullDescription() {
            // Given
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .description(null)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateProfile(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDescription()).isNull();
        }

        @Test
        @DisplayName("Should not change email when updating profile")
        void shouldNotChangeEmail() {
            // Given
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.updateProfile(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(userEmail);
        }

        @Test
        @DisplayName("Should not change password when updating profile")
        void shouldNotChangePassword() {
            // Given
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.updateProfile(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String unknownEmail = "unknown@example.com";
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateProfile(unknownEmail, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(userRepositoryPort, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should successfully change password")
        void shouldChangePassword() {
            // Given
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("newPassword123")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.changePassword(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("newEncodedPassword");

            verify(passwordEncoder).matches("oldPassword", "encodedPassword");
            verify(passwordEncoder).encode("newPassword123");
        }

        @Test
        @DisplayName("Should update the updatedAt timestamp when changing password")
        void shouldUpdateTimestamp() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("newPassword123")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.changePassword(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should throw InvalidPasswordException when current password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongPassword")
                    .newPassword("newPassword123")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(userEmail, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("Current password is incorrect");

            verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepositoryPort, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String unknownEmail = "unknown@example.com";
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("newPassword123")
                    .build();

            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(unknownEmail, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepositoryPort, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should not change email when changing password")
        void shouldNotChangeEmail() {
            // Given
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("newPassword123")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.changePassword(userEmail, request);

            // Then
            verify(userRepositoryPort).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(userEmail);
        }
    }
}
