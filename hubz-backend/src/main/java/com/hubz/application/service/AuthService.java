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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenPort jwtTokenPort;

    public AuthResponse register(RegisterRequest request) {
        if (userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepositoryPort.save(user);
        String token = jwtTokenPort.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepositoryPort.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenPort.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return toUserResponse(user);
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
