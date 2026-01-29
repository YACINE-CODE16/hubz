package com.hubz.presentation.controller;

import com.hubz.application.dto.request.ChangePasswordRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.service.UserService;
import com.hubz.domain.exception.InvalidPasswordException;
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
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
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
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Nested
    @DisplayName("PUT /api/users/me - Update Profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should return 200 and updated user when profile update is valid")
        void shouldUpdateProfile() throws Exception {
            // Given
            UserResponse response = UserResponse.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .description("Updated description")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userService.updateProfile(eq("test@example.com"), any(UpdateProfileRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/users/me")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jane",
                                        "lastName": "Smith",
                                        "description": "Updated description"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(userService).updateProfile(eq("test@example.com"), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameBlank() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/users/me")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "",
                                        "lastName": "Smith",
                                        "description": "Updated"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when lastName is missing")
        void shouldReturn400WhenLastNameMissing() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/users/me")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jane"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("Should return 200 when description is null")
        void shouldAcceptNullDescription() throws Exception {
            // Given
            UserResponse response = UserResponse.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userService.updateProfile(eq("test@example.com"), any(UpdateProfileRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/users/me")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jane",
                                        "lastName": "Smith"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            when(userService.updateProfile(eq("unknown@example.com"), any(UpdateProfileRequest.class)))
                    .thenThrow(new UserNotFoundException("unknown@example.com"));

            // When & Then
            mockMvc.perform(put("/api/users/me")
                            .principal((Principal) () -> "unknown@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jane",
                                        "lastName": "Smith"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me/password - Change Password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should return 204 when password change is successful")
        void shouldChangePassword() throws Exception {
            // Given
            doNothing().when(userService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));

            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "oldPassword123",
                                        "newPassword": "newPassword123"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            verify(userService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when current password is incorrect")
        void shouldReturn400WhenCurrentPasswordIncorrect() throws Exception {
            // Given
            doThrow(new InvalidPasswordException())
                    .when(userService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));

            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "wrongPassword",
                                        "newPassword": "newPassword123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("Should return 400 when currentPassword is blank")
        void shouldReturn400WhenCurrentPasswordBlank() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "",
                                        "newPassword": "newPassword123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when newPassword is too short")
        void shouldReturn400WhenNewPasswordTooShort() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "oldPassword123",
                                        "newPassword": "short"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when newPassword is missing")
        void shouldReturn400WhenNewPasswordMissing() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "test@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "oldPassword123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            doThrow(new UserNotFoundException("unknown@example.com"))
                    .when(userService).changePassword(eq("unknown@example.com"), any(ChangePasswordRequest.class));

            // When & Then
            mockMvc.perform(put("/api/users/me/password")
                            .principal((Principal) () -> "unknown@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currentPassword": "oldPassword123",
                                        "newPassword": "newPassword123"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }
}
