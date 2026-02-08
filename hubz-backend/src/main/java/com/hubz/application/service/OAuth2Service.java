package com.hubz.application.service;

import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.GoogleOAuth2Port;
import com.hubz.application.port.out.JwtTokenPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.OAuth2AuthenticationException;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final GoogleOAuth2Port googleOAuth2Port;
    private final UserRepositoryPort userRepositoryPort;
    private final JwtTokenPort jwtTokenPort;
    private final EmailService emailService;

    /**
     * Returns the Google OAuth2 authorization URL to redirect the user to.
     */
    public String getGoogleAuthorizationUrl() {
        return googleOAuth2Port.buildAuthorizationUrl();
    }

    /**
     * Handles the Google OAuth2 callback.
     * Exchanges the authorization code for user info, then creates or logs in the user.
     *
     * @param authorizationCode the code received from Google
     * @return AuthResponse with JWT token and user info
     */
    public AuthResponse handleGoogleCallback(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new OAuth2AuthenticationException("Authorization code is required");
        }

        Map<String, String> googleUserInfo;
        try {
            googleUserInfo = googleOAuth2Port.exchangeCodeForUserInfo(authorizationCode);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Failed to authenticate with Google: " + e.getMessage(), e);
        }

        String email = googleUserInfo.get("email");
        String firstName = googleUserInfo.get("given_name");
        String lastName = googleUserInfo.get("family_name");
        String googleId = googleUserInfo.get("sub");
        String pictureUrl = googleUserInfo.get("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account does not have an email address");
        }

        if (firstName == null || firstName.isBlank()) {
            firstName = email.split("@")[0];
        }

        if (lastName == null || lastName.isBlank()) {
            lastName = "";
        }

        User user = findOrCreateUser(email, firstName, lastName, googleId, pictureUrl);

        String token = jwtTokenPort.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .requires2FA(false)
                .build();
    }

    /**
     * Finds an existing user by email or creates a new one with Google OAuth2 info.
     */
    private User findOrCreateUser(String email, String firstName, String lastName, String googleId, String pictureUrl) {
        Optional<User> existingUser = userRepositoryPort.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // If user exists but doesn't have OAuth provider set, link the Google account
            if (user.getOauthProvider() == null) {
                user.setOauthProvider("google");
                user.setOauthProviderId(googleId);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepositoryPort.save(user);
            }

            return user;
        }

        // Create new user from Google OAuth2 data
        User newUser = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("")  // OAuth users don't have a password
                .firstName(firstName)
                .lastName(lastName)
                .profilePhotoUrl(pictureUrl)
                .emailVerified(true)  // Google emails are already verified
                .twoFactorEnabled(false)
                .oauthProvider("google")
                .oauthProviderId(googleId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepositoryPort.save(newUser);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
        } catch (Exception e) {
            // Don't fail the OAuth flow if welcome email fails
        }

        return savedUser;
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
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .oauthProvider(user.getOauthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
