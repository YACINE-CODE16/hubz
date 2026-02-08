package com.hubz.presentation.advice;

import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.AccountDeletionException;
import com.hubz.domain.exception.BackgroundJobNotFoundException;
import com.hubz.domain.exception.DirectMessageNotFoundException;
import com.hubz.domain.exception.CannotChangeOwnerRoleException;
import com.hubz.domain.exception.ChecklistItemNotFoundException;
import com.hubz.domain.exception.EmailAlreadyVerifiedException;
import com.hubz.domain.exception.EmailNotVerifiedException;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.exception.EventParticipantNotFoundException;
import com.hubz.domain.exception.GoalNotFoundException;
import com.hubz.domain.exception.HabitLogNotFoundException;
import com.hubz.domain.exception.HabitNotFoundException;
import com.hubz.domain.exception.InvalidCredentialsException;
import com.hubz.domain.exception.InvalidTokenException;
import com.hubz.domain.exception.OAuth2AuthenticationException;
import com.hubz.domain.exception.InvalidTotpCodeException;
import com.hubz.domain.exception.NotificationNotFoundException;
import com.hubz.domain.exception.RateLimitExceededException;
import com.hubz.domain.exception.TagNotFoundException;
import com.hubz.domain.exception.TaskAttachmentNotFoundException;
import com.hubz.domain.exception.InvalidPasswordException;
import com.hubz.domain.exception.MessageNotFoundException;
import com.hubz.domain.exception.MemberAlreadyExistsException;
import com.hubz.domain.exception.MemberNotFoundException;
import com.hubz.domain.exception.NoteFolderNotFoundException;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.exception.NoteTagNotFoundException;
import com.hubz.domain.exception.NoteVersionNotFoundException;
import com.hubz.domain.exception.OrganizationNotFoundException;
import com.hubz.domain.exception.TaskCommentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.exception.TeamMemberAlreadyExistsException;
import com.hubz.domain.exception.TeamNotFoundException;
import com.hubz.domain.exception.TwoFactorAuthException;
import com.hubz.domain.exception.TwoFactorRequiredException;
import com.hubz.domain.exception.UserAlreadyExistsException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.exception.WebhookConfigNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CannotChangeOwnerRoleException.class)
    public ResponseEntity<Map<String, Object>> handleCannotChangeOwnerRole(CannotChangeOwnerRoleException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccountDeletionException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDeletion(AccountDeletionException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMemberNotFound(MemberNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleMemberAlreadyExists(MemberAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TeamMemberAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleTeamMemberAlreadyExists(TeamMemberAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyVerified(EmailAlreadyVerifiedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(InvalidPasswordException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TaskCommentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskCommentNotFound(TaskCommentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChecklistItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleChecklistItemNotFound(ChecklistItemNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TaskAttachmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskAttachmentNotFound(TaskAttachmentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTeamNotFound(TeamNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(GoalNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGoalNotFound(GoalNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEventNotFound(EventNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EventParticipantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEventParticipantNotFound(EventParticipantNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteNotFound(NoteNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoteFolderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteFolderNotFound(NoteFolderNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoteTagNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteTagNotFound(NoteTagNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoteVersionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteVersionNotFound(NoteVersionNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HabitNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleHabitNotFound(HabitNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HabitLogNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleHabitLogNotFound(HabitLogNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTagNotFound(TagNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotFound(MessageNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DirectMessageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDirectMessageNotFound(DirectMessageNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TwoFactorAuthException.class)
    public ResponseEntity<Map<String, Object>> handleTwoFactorAuth(TwoFactorAuthException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TwoFactorRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleTwoFactorRequired(TwoFactorRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", ex.getMessage());
        body.put("requires2FA", true);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(InvalidTotpCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTotpCode(InvalidTotpCodeException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleOAuth2Authentication(OAuth2AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(WebhookConfigNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookConfigNotFound(WebhookConfigNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BackgroundJobNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBackgroundJobNotFound(BackgroundJobNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", ex.getMessage());
        body.put("retryAfter", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
