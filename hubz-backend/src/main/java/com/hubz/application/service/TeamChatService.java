package com.hubz.application.service;

import com.hubz.application.dto.request.CreateMessageRequest;
import com.hubz.application.dto.request.UpdateMessageRequest;
import com.hubz.application.dto.response.ChatMessageResponse;
import com.hubz.application.port.out.MessageRepositoryPort;
import com.hubz.application.port.out.TeamRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.MessageNotFoundException;
import com.hubz.domain.exception.TeamNotFoundException;
import com.hubz.domain.model.Message;
import com.hubz.domain.model.Team;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamChatService {

    private final MessageRepositoryPort messageRepository;
    private final TeamRepositoryPort teamRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DELETED_MESSAGE_CONTENT = "Ce message a ete supprime.";

    @Transactional
    public ChatMessageResponse sendMessage(UUID teamId, CreateMessageRequest request, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        Message message = Message.builder()
                .teamId(teamId)
                .userId(currentUserId)
                .content(request.getContent())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(UUID teamId, int page, int size, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        int effectiveSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

        return messageRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ChatMessageResponse editMessage(UUID messageId, UpdateMessageRequest request, UUID currentUserId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        // Only the author can edit their own message
        if (!message.getUserId().equals(currentUserId)) {
            throw AccessDeniedException.notAuthor();
        }

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID currentUserId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        Team team = teamRepository.findById(message.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException(message.getTeamId()));

        // Author can delete their own message, or organization admin/owner can delete any message
        boolean isAuthor = message.getUserId().equals(currentUserId);
        boolean isAdmin = authorizationService.isOrganizationAdmin(team.getOrganizationId(), currentUserId);

        if (!isAuthor && !isAdmin) {
            throw AccessDeniedException.notAuthor();
        }

        // Soft delete: replace content and mark as deleted
        message.setContent(DELETED_MESSAGE_CONTENT);
        message.setDeleted(true);
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public int getMessageCount(UUID teamId, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        return messageRepository.countByTeamId(teamId);
    }

    private ChatMessageResponse toResponse(Message message) {
        String authorName = "Unknown User";
        String authorProfilePhotoUrl = null;

        if (!message.isDeleted()) {
            User user = userRepository.findById(message.getUserId()).orElse(null);
            if (user != null) {
                authorName = user.getFirstName() + " " + user.getLastName();
                authorProfilePhotoUrl = user.getProfilePhotoUrl();
            }
        }

        boolean edited = message.getEditedAt() != null;

        return ChatMessageResponse.builder()
                .id(message.getId())
                .teamId(message.getTeamId())
                .userId(message.getUserId())
                .authorName(authorName)
                .authorProfilePhotoUrl(authorProfilePhotoUrl)
                .content(message.getContent())
                .deleted(message.isDeleted())
                .edited(edited)
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
