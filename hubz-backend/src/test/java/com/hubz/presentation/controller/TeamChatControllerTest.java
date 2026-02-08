package com.hubz.presentation.controller;

import com.hubz.application.dto.response.ChatMessageResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TeamChatService;
import com.hubz.domain.exception.MessageNotFoundException;
import com.hubz.domain.exception.TeamNotFoundException;
import com.hubz.domain.model.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = TeamChatController.class,
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
@DisplayName("TeamChatController Unit Tests")
class TeamChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamChatService teamChatService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private final UUID teamId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    private Authentication mockAuthentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        return auth;
    }

    private ChatMessageResponse createMessageResponse() {
        return ChatMessageResponse.builder()
                .id(messageId)
                .teamId(teamId)
                .userId(userId)
                .authorName("John Doe")
                .content("Hello team!")
                .deleted(false)
                .edited(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/teams/{teamId}/messages - Send Message")
    class SendMessageTests {

        @Test
        @DisplayName("Should return 201 when message is sent successfully")
        void shouldSendMessage() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            ChatMessageResponse response = createMessageResponse();
            when(teamChatService.sendMessage(eq(teamId), any(), eq(userId))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/teams/" + teamId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Hello team!"
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Hello team!"))
                    .andExpect(jsonPath("$.authorName").value("John Doe"))
                    .andExpect(jsonPath("$.deleted").value(false));
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void shouldReturn400WhenContentBlank() throws Exception {
            // Given
            Authentication auth = mockAuthentication();

            // When & Then
            mockMvc.perform(post("/api/teams/" + teamId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": ""
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isBadRequest());

            verify(teamChatService, never()).sendMessage(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 404 when team not found")
        void shouldReturn404WhenTeamNotFound() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            UUID nonExistentTeamId = UUID.randomUUID();
            when(teamChatService.sendMessage(eq(nonExistentTeamId), any(), eq(userId)))
                    .thenThrow(new TeamNotFoundException(nonExistentTeamId));

            // When & Then
            mockMvc.perform(post("/api/teams/" + nonExistentTeamId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Hello"
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/messages - Get Messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return 200 with paginated messages")
        void shouldGetMessages() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            ChatMessageResponse response = createMessageResponse();
            Page<ChatMessageResponse> page = new PageImpl<>(List.of(response));

            when(teamChatService.getMessages(eq(teamId), eq(0), eq(50), eq(userId))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/teams/" + teamId + "/messages")
                            .param("page", "0")
                            .param("size", "50")
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Hello team!"))
                    .andExpect(jsonPath("$.content[0].authorName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return empty page when no messages")
        void shouldReturnEmptyPage() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            Page<ChatMessageResponse> emptyPage = new PageImpl<>(List.of());

            when(teamChatService.getMessages(eq(teamId), eq(0), eq(50), eq(userId))).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/teams/" + teamId + "/messages")
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/teams/{teamId}/messages/{messageId} - Edit Message")
    class EditMessageTests {

        @Test
        @DisplayName("Should return 200 when message is edited successfully")
        void shouldEditMessage() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .id(messageId)
                    .teamId(teamId)
                    .userId(userId)
                    .authorName("John Doe")
                    .content("Updated content")
                    .deleted(false)
                    .edited(true)
                    .createdAt(LocalDateTime.now())
                    .editedAt(LocalDateTime.now())
                    .build();

            when(teamChatService.editMessage(eq(messageId), any(), eq(userId))).thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/teams/" + teamId + "/messages/" + messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated content"
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Updated content"))
                    .andExpect(jsonPath("$.edited").value(true));
        }

        @Test
        @DisplayName("Should return 404 when message not found")
        void shouldReturn404WhenMessageNotFound() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            UUID nonExistentId = UUID.randomUUID();
            when(teamChatService.editMessage(eq(nonExistentId), any(), eq(userId)))
                    .thenThrow(new MessageNotFoundException(nonExistentId));

            // When & Then
            mockMvc.perform(put("/api/teams/" + teamId + "/messages/" + nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated"
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void shouldReturn400WhenContentBlankOnEdit() throws Exception {
            // Given
            Authentication auth = mockAuthentication();

            // When & Then
            mockMvc.perform(put("/api/teams/" + teamId + "/messages/" + messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": ""
                                    }
                                    """)
                            .principal(auth))
                    .andExpect(status().isBadRequest());

            verify(teamChatService, never()).editMessage(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{teamId}/messages/{messageId} - Delete Message")
    class DeleteMessageTests {

        @Test
        @DisplayName("Should return 204 when message is deleted successfully")
        void shouldDeleteMessage() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            doNothing().when(teamChatService).deleteMessage(messageId, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/" + teamId + "/messages/" + messageId)
                            .principal(auth))
                    .andExpect(status().isNoContent());

            verify(teamChatService).deleteMessage(messageId, userId);
        }

        @Test
        @DisplayName("Should return 404 when message not found on delete")
        void shouldReturn404WhenMessageNotFoundOnDelete() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            UUID nonExistentId = UUID.randomUUID();
            doThrow(new MessageNotFoundException(nonExistentId))
                    .when(teamChatService).deleteMessage(nonExistentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/" + teamId + "/messages/" + nonExistentId)
                            .principal(auth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/messages/count - Get Message Count")
    class GetMessageCountTests {

        @Test
        @DisplayName("Should return 200 with message count")
        void shouldReturnMessageCount() throws Exception {
            // Given
            Authentication auth = mockAuthentication();
            when(teamChatService.getMessageCount(teamId, userId)).thenReturn(42);

            // When & Then
            mockMvc.perform(get("/api/teams/" + teamId + "/messages/count")
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));
        }
    }
}
