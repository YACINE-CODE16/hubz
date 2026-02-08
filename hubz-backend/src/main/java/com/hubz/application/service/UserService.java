package com.hubz.application.service;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.DeleteAccountRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.AccountDeletionException;
import com.hubz.domain.exception.InvalidPasswordException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepositoryPort;
    private final OrganizationMemberRepositoryPort memberRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /**
     * Get a user by their ID. Result is cached in the "users" cache.
     *
     * @param id the user's UUID
     * @return the user response DTO
     */
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(UUID id) {
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toUserResponse(user);
    }

    @CacheEvict(value = "users", key = "#result.id")
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDescription(request.getDescription());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepositoryPort.save(user);

        return toUserResponse(updatedUser);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        userRepositoryPort.save(user);
    }

    /**
     * Upload or update profile photo for a user.
     *
     * @param email the user's email
     * @param file the photo file
     * @return the updated user response
     */
    public UserResponse uploadProfilePhoto(String email, MultipartFile file) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        try {
            String photoPath = fileStorageService.storeProfilePhoto(file, user.getId());
            user.setProfilePhotoUrl(photoPath);
            user.setUpdatedAt(LocalDateTime.now());

            User updatedUser = userRepositoryPort.save(user);
            return toUserResponse(updatedUser);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store profile photo", e);
        }
    }

    /**
     * Delete the profile photo for a user.
     *
     * @param email the user's email
     * @return the updated user response
     */
    public UserResponse deleteProfilePhoto(String email) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.getProfilePhotoUrl() != null) {
            try {
                fileStorageService.deleteProfilePhoto(user.getId());
            } catch (IOException e) {
                // Log but don't fail if file deletion fails
            }
            user.setProfilePhotoUrl(null);
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepositoryPort.save(user);
        }

        return toUserResponse(user);
    }

    /**
     * Delete a user's account after verifying their password.
     * This will:
     * - Remove the user from all organizations they are a member of
     * - Transfer ownership of owned organizations to another admin, or delete if no other members
     * - Delete the user's profile photo
     * - Delete the user account
     *
     * @param email the user's email
     * @param request the delete account request with password confirmation
     */
    @Transactional
    public void deleteAccount(String email, DeleteAccountRequest request) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // Handle organizations where user is owner
        List<OrganizationMember> memberships = memberRepositoryPort.findByUserId(user.getId());
        for (OrganizationMember membership : memberships) {
            if (membership.getRole() == MemberRole.OWNER) {
                handleOrganizationOwnershipTransfer(membership.getOrganizationId(), user.getId());
            }
        }

        // Remove user from all organizations
        memberRepositoryPort.deleteAllByUserId(user.getId());

        // Delete profile photo if exists
        if (user.getProfilePhotoUrl() != null) {
            try {
                fileStorageService.deleteProfilePhoto(user.getId());
            } catch (IOException e) {
                // Log but don't fail
            }
        }

        // Delete user account
        userRepositoryPort.deleteById(user.getId());
    }

    /**
     * Handle ownership transfer when an owner deletes their account.
     * Will try to transfer to an admin, or delete the organization if no other members.
     */
    private void handleOrganizationOwnershipTransfer(java.util.UUID organizationId, java.util.UUID leavingOwnerId) {
        List<OrganizationMember> members = memberRepositoryPort.findByOrganizationId(organizationId);

        // Filter out the leaving owner
        List<OrganizationMember> remainingMembers = members.stream()
                .filter(m -> !m.getUserId().equals(leavingOwnerId))
                .toList();

        if (remainingMembers.isEmpty()) {
            // No other members, delete the organization
            organizationRepositoryPort.deleteById(organizationId);
        } else {
            // Try to find an admin to promote, otherwise promote any member
            OrganizationMember newOwner = remainingMembers.stream()
                    .filter(m -> m.getRole() == MemberRole.ADMIN)
                    .findFirst()
                    .orElse(remainingMembers.get(0));

            // Promote to owner
            newOwner.setRole(MemberRole.OWNER);
            memberRepositoryPort.save(newOwner);

            // Update organization's owner field
            Organization org = organizationRepositoryPort.findById(organizationId)
                    .orElseThrow(() -> new AccountDeletionException("Organization not found during ownership transfer"));
            org.setOwnerId(newOwner.getUserId());
            organizationRepositoryPort.save(org);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .description(user.getDescription())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .emailVerified(user.getEmailVerified())
                .oauthProvider(user.getOauthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
