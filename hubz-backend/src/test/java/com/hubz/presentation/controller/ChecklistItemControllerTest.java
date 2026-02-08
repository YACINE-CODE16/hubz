package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateChecklistItemRequest;
import com.hubz.application.dto.request.ReorderChecklistItemsRequest;
import com.hubz.application.dto.request.UpdateChecklistItemRequest;
import com.hubz.application.dto.response.ChecklistItemResponse;
import com.hubz.application.dto.response.ChecklistProgressResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ChecklistItemService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.ChecklistItemNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ChecklistItemController.class,
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
@DisplayName("ChecklistItemController Unit Tests")
class ChecklistItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChecklistItemService checklistService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID taskId;
    private UUID itemId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    private ChecklistItemResponse createItemResponse(int position, boolean completed) {
        return ChecklistItemResponse.builder()
                .id(itemId)
                .taskId(taskId)
                .content("Test checklist item")
                .completed(completed)
                .position(position)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/checklist - Get Checklist")
    class GetChecklistTests {

        @Test
        @DisplayName("Should return 200 and checklist with progress")
        void shouldGetChecklist() throws Exception {
            // Given
            ChecklistProgressResponse response = ChecklistProgressResponse.builder()
                    .taskId(taskId)
                    .totalItems(3)
                    .completedItems(2)
                    .completionPercentage(66.67)
                    .items(List.of(
                            createItemResponse(0, true),
                            createItemResponse(1, true),
                            createItemResponse(2, false)
                    ))
                    .build();

            when(checklistService.getChecklist(taskId, userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.taskId").value(taskId.toString()))
                    .andExpect(jsonPath("$.totalItems").value(3))
                    .andExpect(jsonPath("$.completedItems").value(2))
                    .andExpect(jsonPath("$.completionPercentage").value(66.67))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(3));

            verify(checklistService).getChecklist(taskId, userId);
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            when(checklistService.getChecklist(taskId, userId))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/tasks/{taskId}/checklist - Create Checklist Item")
    class CreateItemTests {

        @Test
        @DisplayName("Should return 201 and created item")
        void shouldCreateItem() throws Exception {
            // Given
            ChecklistItemResponse response = createItemResponse(0, false);
            when(checklistService.create(eq(taskId), any(CreateChecklistItemRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Test checklist item"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Test checklist item"))
                    .andExpect(jsonPath("$.completed").value(false));

            verify(checklistService).create(eq(taskId), any(CreateChecklistItemRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 201 when creating item with position")
        void shouldCreateItemWithPosition() throws Exception {
            // Given
            ChecklistItemResponse response = ChecklistItemResponse.builder()
                    .id(itemId)
                    .taskId(taskId)
                    .content("Item at position 5")
                    .completed(false)
                    .position(5)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(checklistService.create(eq(taskId), any(CreateChecklistItemRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Item at position 5",
                                        "position": 5
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.position").value(5));

            verify(checklistService).create(eq(taskId), any(CreateChecklistItemRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void shouldReturn400WhenContentBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(checklistService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            when(checklistService.create(eq(taskId), any(CreateChecklistItemRequest.class), eq(userId)))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Test checklist item"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{taskId}/checklist/{itemId} - Update Checklist Item")
    class UpdateItemTests {

        @Test
        @DisplayName("Should return 200 and updated item")
        void shouldUpdateItem() throws Exception {
            // Given
            ChecklistItemResponse response = ChecklistItemResponse.builder()
                    .id(itemId)
                    .taskId(taskId)
                    .content("Updated content")
                    .completed(true)
                    .position(0)
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(checklistService.update(eq(itemId), any(UpdateChecklistItemRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/checklist/{itemId}", taskId, itemId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated content",
                                        "completed": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Updated content"))
                    .andExpect(jsonPath("$.completed").value(true));

            verify(checklistService).update(eq(itemId), any(UpdateChecklistItemRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when item not found")
        void shouldReturn404WhenItemNotFound() throws Exception {
            // Given
            when(checklistService.update(eq(itemId), any(UpdateChecklistItemRequest.class), eq(userId)))
                    .thenThrow(new ChecklistItemNotFoundException(itemId));

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/checklist/{itemId}", taskId, itemId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated content"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/tasks/{taskId}/checklist/{itemId}/toggle - Toggle Item")
    class ToggleItemTests {

        @Test
        @DisplayName("Should return 200 and toggled item")
        void shouldToggleItem() throws Exception {
            // Given
            ChecklistItemResponse response = createItemResponse(0, true);
            when(checklistService.toggleCompleted(itemId, userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, itemId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));

            verify(checklistService).toggleCompleted(itemId, userId);
        }

        @Test
        @DisplayName("Should return 404 when item not found")
        void shouldReturn404WhenItemNotFound() throws Exception {
            // Given
            when(checklistService.toggleCompleted(itemId, userId))
                    .thenThrow(new ChecklistItemNotFoundException(itemId));

            // When & Then
            mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, itemId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{taskId}/checklist/reorder - Reorder Items")
    class ReorderItemsTests {

        @Test
        @DisplayName("Should return 200 and reordered items")
        void shouldReorderItems() throws Exception {
            // Given
            UUID item1Id = UUID.randomUUID();
            UUID item2Id = UUID.randomUUID();
            UUID item3Id = UUID.randomUUID();

            List<ChecklistItemResponse> response = Arrays.asList(
                    ChecklistItemResponse.builder().id(item3Id).position(0).content("Item 3").taskId(taskId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                    ChecklistItemResponse.builder().id(item1Id).position(1).content("Item 1").taskId(taskId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                    ChecklistItemResponse.builder().id(item2Id).position(2).content("Item 2").taskId(taskId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
            );

            when(checklistService.reorder(eq(taskId), any(ReorderChecklistItemsRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/checklist/reorder", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "itemIds": ["%s", "%s", "%s"]
                                    }
                                    """, item3Id, item1Id, item2Id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3));

            verify(checklistService).reorder(eq(taskId), any(ReorderChecklistItemsRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when itemIds is empty")
        void shouldReturn400WhenItemIdsEmpty() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/checklist/reorder", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "itemIds": []
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(checklistService, never()).reorder(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{taskId}/checklist/{itemId} - Delete Item")
    class DeleteItemTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteItem() throws Exception {
            // Given
            doNothing().when(checklistService).delete(itemId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/checklist/{itemId}", taskId, itemId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(checklistService).delete(itemId, userId);
        }

        @Test
        @DisplayName("Should return 404 when item not found")
        void shouldReturn404WhenItemNotFound() throws Exception {
            // Given
            doThrow(new ChecklistItemNotFoundException(itemId))
                    .when(checklistService).delete(itemId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/checklist/{itemId}", taskId, itemId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(AccessDeniedException.notMember())
                    .when(checklistService).delete(itemId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/checklist/{itemId}", taskId, itemId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
