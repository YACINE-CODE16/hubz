package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTaskCommentRequest;
import com.hubz.application.dto.request.UpdateTaskCommentRequest;
import com.hubz.application.dto.response.TaskCommentResponse;
import com.hubz.application.port.out.TaskCommentRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.TaskCommentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskComment;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskCommentService Unit Tests")
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepositoryPort commentRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TaskCommentService commentService;

    private UUID taskId;
    private UUID userId;
    private UUID organizationId;
    private UUID commentId;
    private Task testTask;
    private TaskComment testComment;
    private User testUser;
    private CreateTaskCommentRequest createRequest;
    private UpdateTaskCommentRequest updateRequest;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LocalDateTime now = LocalDateTime.now();
        testComment = TaskComment.builder()
                .id(commentId)
                .taskId(taskId)
                .authorId(userId)
                .content("This is a test comment")
                .parentCommentId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        createRequest = new CreateTaskCommentRequest();
        createRequest.setContent("This is a new comment");

        updateRequest = new UpdateTaskCommentRequest();
        updateRequest.setContent("This is an updated comment");
    }

    @Nested
    @DisplayName("Get Comments Tests")
    class GetCommentsTests {

        @Test
        @DisplayName("Should successfully get comments by task")
        void shouldGetCommentsByTask() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId)).thenReturn(List.of(testComment));
            when(commentRepository.findByTaskId(taskId)).thenReturn(List.of(testComment));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TaskCommentResponse> comments = commentService.getCommentsByTask(taskId, userId);

            // Then
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getContent()).isEqualTo(testComment.getContent());
            assertThat(comments.get(0).getAuthorId()).isEqualTo(userId);
            verify(taskRepository).findById(taskId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no comments exist")
        void shouldReturnEmptyListWhenNoComments() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId)).thenReturn(List.of());
            when(commentRepository.findByTaskId(taskId)).thenReturn(List.of());

            // When
            List<TaskCommentResponse> comments = commentService.getCommentsByTask(taskId, userId);

            // Then
            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.getCommentsByTask(nonExistentTaskId, userId))
                    .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doThrow(AccessDeniedException.notMember())
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> commentService.getCommentsByTask(taskId, userId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should include nested replies in response")
        void shouldIncludeNestedReplies() {
            // Given
            UUID replyId = UUID.randomUUID();
            TaskComment reply = TaskComment.builder()
                    .id(replyId)
                    .taskId(taskId)
                    .authorId(userId)
                    .content("This is a reply")
                    .parentCommentId(commentId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId)).thenReturn(List.of(testComment));
            when(commentRepository.findByTaskId(taskId)).thenReturn(List.of(testComment, reply));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TaskCommentResponse> comments = commentService.getCommentsByTask(taskId, userId);

            // Then
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getReplies()).hasSize(1);
            assertThat(comments.get(0).getReplies().get(0).getContent()).isEqualTo("This is a reply");
        }
    }

    @Nested
    @DisplayName("Get Comment Count Tests")
    class GetCommentCountTests {

        @Test
        @DisplayName("Should return correct comment count")
        void shouldReturnCorrectCommentCount() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.countByTaskId(taskId)).thenReturn(5);

            // When
            int count = commentService.getCommentCount(taskId, userId);

            // Then
            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero when no comments")
        void shouldReturnZeroWhenNoComments() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.countByTaskId(taskId)).thenReturn(0);

            // When
            int count = commentService.getCommentCount(taskId, userId);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Create Comment Tests")
    class CreateCommentTests {

        @Test
        @DisplayName("Should successfully create comment")
        void shouldCreateComment() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.save(any(TaskComment.class))).thenReturn(testComment);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TaskCommentResponse response = commentService.createComment(taskId, createRequest, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(testComment.getContent());
            assertThat(response.getAuthorId()).isEqualTo(userId);
            verify(commentRepository).save(any(TaskComment.class));
        }

        @Test
        @DisplayName("Should set timestamps when creating comment")
        void shouldSetTimestamps() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<TaskComment> commentCaptor = ArgumentCaptor.forClass(TaskComment.class);
            when(commentRepository.save(commentCaptor.capture())).thenReturn(testComment);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            commentService.createComment(taskId, createRequest, userId);

            // Then
            TaskComment savedComment = commentCaptor.getValue();
            assertThat(savedComment.getCreatedAt()).isNotNull();
            assertThat(savedComment.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create reply to existing comment")
        void shouldCreateReplyToExistingComment() {
            // Given
            CreateTaskCommentRequest replyRequest = new CreateTaskCommentRequest();
            replyRequest.setContent("This is a reply");
            replyRequest.setParentCommentId(commentId);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            TaskComment reply = TaskComment.builder()
                    .id(UUID.randomUUID())
                    .taskId(taskId)
                    .authorId(userId)
                    .content("This is a reply")
                    .parentCommentId(commentId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(commentRepository.save(any(TaskComment.class))).thenReturn(reply);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TaskCommentResponse response = commentService.createComment(taskId, replyRequest, userId);

            // Then
            assertThat(response.getParentCommentId()).isEqualTo(commentId);
        }

        @Test
        @DisplayName("Should throw exception when parent comment not found")
        void shouldThrowExceptionWhenParentCommentNotFound() {
            // Given
            UUID nonExistentParentId = UUID.randomUUID();
            CreateTaskCommentRequest replyRequest = new CreateTaskCommentRequest();
            replyRequest.setContent("This is a reply");
            replyRequest.setParentCommentId(nonExistentParentId);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.createComment(taskId, replyRequest, userId))
                    .isInstanceOf(TaskCommentNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when parent comment belongs to different task")
        void shouldThrowExceptionWhenParentCommentBelongsToDifferentTask() {
            // Given
            UUID differentTaskId = UUID.randomUUID();
            TaskComment commentOnDifferentTask = TaskComment.builder()
                    .id(UUID.randomUUID())
                    .taskId(differentTaskId)
                    .authorId(userId)
                    .content("Comment on different task")
                    .build();

            CreateTaskCommentRequest replyRequest = new CreateTaskCommentRequest();
            replyRequest.setContent("This is a reply");
            replyRequest.setParentCommentId(commentOnDifferentTask.getId());

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findById(commentOnDifferentTask.getId())).thenReturn(Optional.of(commentOnDifferentTask));

            // When & Then
            assertThatThrownBy(() -> commentService.createComment(taskId, replyRequest, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Parent comment does not belong to this task");
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.createComment(nonExistentTaskId, createRequest, userId))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Comment Tests")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should successfully update comment")
        void shouldUpdateComment() {
            // Given
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            TaskComment updatedComment = TaskComment.builder()
                    .id(commentId)
                    .taskId(taskId)
                    .authorId(userId)
                    .content(updateRequest.getContent())
                    .createdAt(testComment.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(commentRepository.save(any(TaskComment.class))).thenReturn(updatedComment);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TaskCommentResponse response = commentService.updateComment(commentId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(commentRepository).save(any(TaskComment.class));
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(commentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.updateComment(nonExistentId, updateRequest, userId))
                    .isInstanceOf(TaskCommentNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user is not the author")
        void shouldThrowExceptionWhenUserIsNotAuthor() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            // When & Then
            assertThatThrownBy(() -> commentService.updateComment(commentId, updateRequest, differentUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should mark comment as edited")
        void shouldMarkCommentAsEdited() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
            TaskComment oldComment = TaskComment.builder()
                    .id(commentId)
                    .taskId(taskId)
                    .authorId(userId)
                    .content("Original content")
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(oldComment));

            TaskComment updatedComment = TaskComment.builder()
                    .id(commentId)
                    .taskId(taskId)
                    .authorId(userId)
                    .content(updateRequest.getContent())
                    .createdAt(createdAt)
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(commentRepository.save(any(TaskComment.class))).thenReturn(updatedComment);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            TaskCommentResponse response = commentService.updateComment(commentId, updateRequest, userId);

            // Then
            assertThat(response.isEdited()).isTrue();
        }
    }

    @Nested
    @DisplayName("Delete Comment Tests")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should successfully delete comment by author")
        void shouldDeleteCommentByAuthor() {
            // Given
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(authorizationService.isOrganizationAdmin(organizationId, userId)).thenReturn(false);
            when(commentRepository.findByParentCommentId(commentId)).thenReturn(List.of());
            doNothing().when(commentRepository).delete(testComment);

            // When
            commentService.deleteComment(commentId, userId);

            // Then
            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("Should successfully delete comment by admin")
        void shouldDeleteCommentByAdmin() {
            // Given
            UUID adminId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(authorizationService.isOrganizationAdmin(organizationId, adminId)).thenReturn(true);
            when(commentRepository.findByParentCommentId(commentId)).thenReturn(List.of());
            doNothing().when(commentRepository).delete(testComment);

            // When
            commentService.deleteComment(commentId, adminId);

            // Then
            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(commentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.deleteComment(nonExistentId, userId))
                    .isInstanceOf(TaskCommentNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user is not author or admin")
        void shouldThrowExceptionWhenUserIsNotAuthorOrAdmin() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(authorizationService.isOrganizationAdmin(organizationId, differentUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> commentService.deleteComment(commentId, differentUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should delete replies when deleting parent comment")
        void shouldDeleteRepliesWhenDeletingParentComment() {
            // Given
            TaskComment reply = TaskComment.builder()
                    .id(UUID.randomUUID())
                    .taskId(taskId)
                    .authorId(userId)
                    .content("Reply")
                    .parentCommentId(commentId)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(authorizationService.isOrganizationAdmin(organizationId, userId)).thenReturn(false);
            when(commentRepository.findByParentCommentId(commentId)).thenReturn(List.of(reply));
            when(commentRepository.findByParentCommentId(reply.getId())).thenReturn(List.of());
            doNothing().when(commentRepository).delete(any(TaskComment.class));

            // When
            commentService.deleteComment(commentId, userId);

            // Then
            verify(commentRepository).delete(reply);
            verify(commentRepository).delete(testComment);
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map comment to response")
        void shouldCorrectlyMapCommentToResponse() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId)).thenReturn(List.of(testComment));
            when(commentRepository.findByTaskId(taskId)).thenReturn(List.of(testComment));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TaskCommentResponse> comments = commentService.getCommentsByTask(taskId, userId);

            // Then
            TaskCommentResponse response = comments.get(0);
            assertThat(response.getId()).isEqualTo(testComment.getId());
            assertThat(response.getTaskId()).isEqualTo(testComment.getTaskId());
            assertThat(response.getAuthorId()).isEqualTo(testComment.getAuthorId());
            assertThat(response.getAuthorName()).isEqualTo("John Doe");
            assertThat(response.getContent()).isEqualTo(testComment.getContent());
            assertThat(response.getCreatedAt()).isEqualTo(testComment.getCreatedAt());
        }

        @Test
        @DisplayName("Should return Unknown User when user not found")
        void shouldReturnUnknownUserWhenUserNotFound() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId)).thenReturn(List.of(testComment));
            when(commentRepository.findByTaskId(taskId)).thenReturn(List.of(testComment));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            List<TaskCommentResponse> comments = commentService.getCommentsByTask(taskId, userId);

            // Then
            assertThat(comments.get(0).getAuthorName()).isEqualTo("Unknown User");
        }
    }
}
