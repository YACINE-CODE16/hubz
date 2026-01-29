package com.hubz.application.service;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidPasswordException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

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

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .description(user.getDescription())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
