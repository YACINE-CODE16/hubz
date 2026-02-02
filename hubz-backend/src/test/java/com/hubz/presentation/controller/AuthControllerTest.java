package com.hubz.presentation.controller;

import com.hubz.application.dto.request.LoginRequest;
import com.hubz.application.dto.request.RegisterRequest;
import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.service.AuthService;
import com.hubz.domain.exception.InvalidCredentialsException;
import com.hubz.domain.exception.UserAlreadyExistsException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private UserResponse createUserResponse() {
        return UserResponse.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register - Register User")
    class RegisterTests {

        @Test
        @DisplayName("Should return 201 and auth response when registration is successful")
        void shouldRegisterUser() throws Exception {
            // Given
            AuthResponse response = AuthResponse.builder()
                    .token("jwt-token-here")
                    .user(createUserResponse())
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123",
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt-token-here"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invalid-email",
                                        "password": "password123",
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "",
                                        "password": "password123",
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "short",
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123",
                                        "firstName": "",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when lastName is missing")
        void shouldReturn400WhenLastNameMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123",
                                        "firstName": "John"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 409 when user already exists")
        void shouldReturn409WhenUserAlreadyExists() throws Exception {
            // Given
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("test@example.com"));

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123",
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should accept optional description field")
        void shouldAcceptOptionalDescription() throws Exception {
            // Given
            AuthResponse response = AuthResponse.builder()
                    .token("jwt-token-here")
                    .user(createUserResponse())
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123",
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "description": "A test user"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            verify(authService).register(any(RegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login - Login User")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 and auth response when login is successful")
        void shouldLoginUser() throws Exception {
            // Given
            AuthResponse response = AuthResponse.builder()
                    .token("jwt-token-here")
                    .user(createUserResponse())
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-here"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invalid-email",
                                        "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "",
                                        "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void shouldReturn400WhenPasswordBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            // Given
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException());

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "wrongpassword"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me - Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return 200 and user response when authenticated")
        void shouldGetCurrentUser() throws Exception {
            // Given
            UserResponse response = createUserResponse();

            when(authService.getCurrentUser("test@example.com")).thenReturn(response);

            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getName()).thenReturn("test@example.com");

            // When & Then
            mockMvc.perform(get("/api/auth/me")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));

            verify(authService).getCurrentUser("test@example.com");
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            when(authService.getCurrentUser("unknown@example.com"))
                    .thenThrow(new UserNotFoundException("unknown@example.com"));

            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getName()).thenReturn("unknown@example.com");

            // When & Then
            mockMvc.perform(get("/api/auth/me")
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }
}
