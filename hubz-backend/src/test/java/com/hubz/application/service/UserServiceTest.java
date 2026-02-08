package com.hubz.application.service;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.DeleteAccountRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.InvalidPasswordException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private OrganizationMemberRepositoryPort memberRepositoryPort;

    @Mock
    private OrganizationRepositoryPort organizationRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

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

    @Nested
    @DisplayName("Profile Photo Tests")
    class ProfilePhotoTests {

        @Test
        @DisplayName("Should successfully upload profile photo")
        void shouldUploadProfilePhoto() throws IOException {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );
            String expectedPath = "profile-photos/" + testUser.getId() + ".jpg";

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(fileStorageService.storeProfilePhoto(file, testUser.getId())).thenReturn(expectedPath);
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.uploadProfilePhoto(userEmail, file);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProfilePhotoUrl()).isEqualTo(expectedPath);

            verify(fileStorageService).storeProfilePhoto(file, testUser.getId());
            verify(userRepositoryPort).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when uploading photo for unknown user")
        void shouldThrowExceptionWhenUserNotFoundForUpload() {
            // Given
            String unknownEmail = "unknown@example.com";
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.uploadProfilePhoto(unknownEmail, file))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verifyNoInteractions(fileStorageService);
        }

        @Test
        @DisplayName("Should successfully delete profile photo")
        void shouldDeleteProfilePhoto() throws IOException {
            // Given
            testUser.setProfilePhotoUrl("profile-photos/" + testUser.getId() + ".jpg");

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.deleteProfilePhoto(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProfilePhotoUrl()).isNull();

            verify(fileStorageService).deleteProfilePhoto(testUser.getId());
            verify(userRepositoryPort).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle deletion when no photo exists")
        void shouldHandleDeletionWhenNoPhotoExists() {
            // Given
            testUser.setProfilePhotoUrl(null);

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

            // When
            UserResponse response = userService.deleteProfilePhoto(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProfilePhotoUrl()).isNull();

            verifyNoInteractions(fileStorageService);
        }
    }

    @Nested
    @DisplayName("Delete Account Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should successfully delete account with no organizations")
        void shouldDeleteAccountWithNoOrganizations() {
            // Given
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
            when(memberRepositoryPort.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());

            // When
            userService.deleteAccount(userEmail, request);

            // Then
            verify(passwordEncoder).matches("correctPassword", "encodedPassword");
            verify(memberRepositoryPort).deleteAllByUserId(testUser.getId());
            verify(userRepositoryPort).deleteById(testUser.getId());
        }

        @Test
        @DisplayName("Should throw InvalidPasswordException when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("wrongPassword")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.deleteAccount(userEmail, request))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
            verify(memberRepositoryPort, never()).deleteAllByUserId(any());
            verify(userRepositoryPort, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should transfer ownership when deleting owner account")
        void shouldTransferOwnershipWhenDeletingOwnerAccount() {
            // Given
            UUID orgId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .build();

            OrganizationMember ownerMembership = OrganizationMember.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .userId(testUser.getId())
                    .role(MemberRole.OWNER)
                    .build();

            OrganizationMember adminMembership = OrganizationMember.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .userId(adminId)
                    .role(MemberRole.ADMIN)
                    .build();

            Organization org = Organization.builder()
                    .id(orgId)
                    .name("Test Org")
                    .ownerId(testUser.getId())
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
            when(memberRepositoryPort.findByUserId(testUser.getId())).thenReturn(List.of(ownerMembership));
            when(memberRepositoryPort.findByOrganizationId(orgId)).thenReturn(List.of(ownerMembership, adminMembership));
            when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(org));
            when(memberRepositoryPort.save(any(OrganizationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(organizationRepositoryPort.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.deleteAccount(userEmail, request);

            // Then
            ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
            verify(memberRepositoryPort).save(memberCaptor.capture());
            OrganizationMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRole()).isEqualTo(MemberRole.OWNER);
            assertThat(savedMember.getUserId()).isEqualTo(adminId);

            ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepositoryPort).save(orgCaptor.capture());
            Organization savedOrg = orgCaptor.getValue();
            assertThat(savedOrg.getOwnerId()).isEqualTo(adminId);

            verify(memberRepositoryPort).deleteAllByUserId(testUser.getId());
            verify(userRepositoryPort).deleteById(testUser.getId());
        }

        @Test
        @DisplayName("Should delete organization when owner deletes and no other members")
        void shouldDeleteOrganizationWhenNoOtherMembers() {
            // Given
            UUID orgId = UUID.randomUUID();

            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .build();

            OrganizationMember ownerMembership = OrganizationMember.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .userId(testUser.getId())
                    .role(MemberRole.OWNER)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
            when(memberRepositoryPort.findByUserId(testUser.getId())).thenReturn(List.of(ownerMembership));
            when(memberRepositoryPort.findByOrganizationId(orgId)).thenReturn(List.of(ownerMembership));

            // When
            userService.deleteAccount(userEmail, request);

            // Then
            verify(organizationRepositoryPort).deleteById(orgId);
            verify(memberRepositoryPort).deleteAllByUserId(testUser.getId());
            verify(userRepositoryPort).deleteById(testUser.getId());
        }

        @Test
        @DisplayName("Should delete profile photo when deleting account")
        void shouldDeleteProfilePhotoWhenDeletingAccount() throws IOException {
            // Given
            testUser.setProfilePhotoUrl("profile-photos/" + testUser.getId() + ".jpg");

            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
            when(memberRepositoryPort.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());

            // When
            userService.deleteAccount(userEmail, request);

            // Then
            verify(fileStorageService).deleteProfilePhoto(testUser.getId());
            verify(userRepositoryPort).deleteById(testUser.getId());
        }
    }
}
