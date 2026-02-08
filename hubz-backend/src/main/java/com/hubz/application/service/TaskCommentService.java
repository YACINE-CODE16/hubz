package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTaskCommentRequest;
import com.hubz.application.dto.request.UpdateTaskCommentRequest;
import com.hubz.application.dto.response.TaskCommentResponse;
import com.hubz.application.port.out.TaskCommentRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.TaskCommentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskComment;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepositoryPort commentRepository;
    private final TaskRepositoryPort taskRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;
    private final MentionService mentionService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getCommentsByTask(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        // Check if user has access to the task's organization
        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        // Get top-level comments only (not replies)
        List<TaskComment> topLevelComments = commentRepository.findByTaskIdAndParentCommentIdIsNull(taskId);

        // Build a map of parent ID -> replies for efficient lookup
        List<TaskComment> allComments = commentRepository.findByTaskId(taskId);
        Map<UUID, List<TaskComment>> repliesMap = allComments.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.groupingBy(TaskComment::getParentCommentId));

        // Convert to responses with nested replies
        return topLevelComments.stream()
                .map(comment -> toResponseWithReplies(comment, repliesMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public int getCommentCount(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        return commentRepository.countByTaskId(taskId);
    }

    @Transactional
    public TaskCommentResponse createComment(UUID taskId, CreateTaskCommentRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        // If replying to a comment, verify parent comment exists and belongs to same task
        if (request.getParentCommentId() != null) {
            TaskComment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new TaskCommentNotFoundException(request.getParentCommentId()));

            if (!parentComment.getTaskId().equals(taskId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this task");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        TaskComment comment = TaskComment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .authorId(currentUserId)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        TaskComment savedComment = commentRepository.save(comment);

        // Process @mentions and send notifications
        processMentions(request.getContent(), task, currentUserId);

        return toResponse(savedComment);
    }

    /**
     * Process @mentions in the comment content and send notifications to mentioned users.
     */
    private void processMentions(String content, Task task, UUID authorId) {
        if (content == null || content.isBlank()) {
            return;
        }

        // Get the author's name for the notification message
        String authorName = userRepository.findById(authorId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Quelqu'un");

        // Parse mentions and resolve to user IDs (excluding the author)
        Set<UUID> mentionedUserIds = mentionService.parseMentionsAndResolve(
                content,
                task.getOrganizationId(),
                authorId
        );

        // Send notifications to all mentioned users
        for (UUID mentionedUserId : mentionedUserIds) {
            notificationService.notifyMention(
                    mentionedUserId,
                    authorName,
                    task.getId(),
                    task.getTitle(),
                    task.getOrganizationId()
            );
        }
    }

    @Transactional
    public TaskCommentResponse updateComment(UUID commentId, UpdateTaskCommentRequest request, UUID currentUserId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new TaskCommentNotFoundException(commentId));

        // Only the author can edit their own comment
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw AccessDeniedException.notAuthor();
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());

        TaskComment savedComment = commentRepository.save(comment);
        return toResponse(savedComment);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID currentUserId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new TaskCommentNotFoundException(commentId));

        Task task = taskRepository.findById(comment.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(comment.getTaskId()));

        // Author can delete their own comment, or organization admin/owner can delete any comment
        boolean isAuthor = comment.getAuthorId().equals(currentUserId);
        boolean isAdmin = authorizationService.isOrganizationAdmin(task.getOrganizationId(), currentUserId);

        if (!isAuthor && !isAdmin) {
            throw AccessDeniedException.notAuthor();
        }

        // Delete all replies first (recursive delete)
        deleteReplies(commentId);

        // Delete the comment
        commentRepository.delete(comment);
    }

    private void deleteReplies(UUID parentCommentId) {
        List<TaskComment> replies = commentRepository.findByParentCommentId(parentCommentId);
        for (TaskComment reply : replies) {
            deleteReplies(reply.getId()); // Recursively delete nested replies
            commentRepository.delete(reply);
        }
    }

    private TaskCommentResponse toResponse(TaskComment comment) {
        String authorName = userRepository.findById(comment.getAuthorId())
                .map(User::getFirstName)
                .map(firstName -> {
                    User user = userRepository.findById(comment.getAuthorId()).orElse(null);
                    return user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown";
                })
                .orElse("Unknown User");

        boolean edited = comment.getUpdatedAt() != null &&
                comment.getCreatedAt() != null &&
                !comment.getUpdatedAt().equals(comment.getCreatedAt());

        return TaskCommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .authorId(comment.getAuthorId())
                .authorName(authorName)
                .content(comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .replies(new ArrayList<>())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .edited(edited)
                .build();
    }

    private TaskCommentResponse toResponseWithReplies(TaskComment comment, Map<UUID, List<TaskComment>> repliesMap) {
        TaskCommentResponse response = toResponse(comment);

        List<TaskComment> replies = repliesMap.getOrDefault(comment.getId(), new ArrayList<>());
        List<TaskCommentResponse> replyResponses = replies.stream()
                .map(reply -> toResponseWithReplies(reply, repliesMap))
                .toList();

        response.setReplies(new ArrayList<>(replyResponses));
        return response;
    }
}
