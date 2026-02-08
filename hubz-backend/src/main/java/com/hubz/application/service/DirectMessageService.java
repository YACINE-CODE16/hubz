package com.hubz.application.service;

import com.hubz.application.dto.request.SendDirectMessageRequest;
import com.hubz.application.dto.request.UpdateDirectMessageRequest;
import com.hubz.application.dto.response.ConversationResponse;
import com.hubz.application.dto.response.DirectMessageResponse;
import com.hubz.application.dto.response.UnreadCountResponse;
import com.hubz.application.port.out.DirectMessageRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.NotificationType;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.DirectMessageNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.DirectMessage;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectMessageRepositoryPort directMessageRepository;
    private final UserRepositoryPort userRepository;
    private final NotificationService notificationService;

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DELETED_MESSAGE_CONTENT = "Ce message a ete supprime.";

    @Transactional
    public DirectMessageResponse sendMessage(SendDirectMessageRequest request, UUID senderId) {
        // Validate sender exists
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        // Validate receiver exists
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException(request.getReceiverId()));

        // Cannot send message to yourself
        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }

        DirectMessage message = DirectMessage.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .read(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        DirectMessage saved = directMessageRepository.save(message);

        // Notify the receiver
        notificationService.createNotification(
                receiver.getId(),
                NotificationType.DIRECT_MESSAGE,
                "Nouveau message",
                sender.getFirstName() + " " + sender.getLastName() + " vous a envoye un message.",
                "/personal/messages?user=" + senderId,
                saved.getId(),
                null
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DirectMessageResponse> getConversation(UUID userId, UUID otherUserId, int page, int size) {
        // Validate both users exist
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new UserNotFoundException(otherUserId));

        int effectiveSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

        return directMessageRepository.findConversation(userId, otherUserId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void markAsRead(UUID messageId, UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new DirectMessageNotFoundException(messageId));

        // Only the receiver can mark a message as read
        if (!message.getReceiverId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can mark a message as read");
        }

        if (!message.isRead()) {
            message.setRead(true);
            directMessageRepository.save(message);
        }
    }

    @Transactional
    public void markConversationAsRead(UUID userId, UUID otherUserId) {
        directMessageRepository.markConversationAsRead(userId, otherUserId);
    }

    @Transactional
    public DirectMessageResponse editMessage(UUID messageId, UpdateDirectMessageRequest request, UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new DirectMessageNotFoundException(messageId));

        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        // Only the sender can edit their own message
        if (!message.getSenderId().equals(userId)) {
            throw AccessDeniedException.notAuthor();
        }

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());

        DirectMessage saved = directMessageRepository.save(message);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new DirectMessageNotFoundException(messageId));

        // Only the sender can delete their own message
        if (!message.getSenderId().equals(userId)) {
            throw AccessDeniedException.notAuthor();
        }

        // Soft delete: replace content and mark as deleted
        message.setContent(DELETED_MESSAGE_CONTENT);
        message.setDeleted(true);
        directMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<DirectMessage> latestMessages = directMessageRepository.findLatestMessagePerConversation(userId);

        return latestMessages.stream()
                .map(message -> {
                    // The "other user" is the one who is not the current user
                    UUID otherUserId = message.getSenderId().equals(userId)
                            ? message.getReceiverId()
                            : message.getSenderId();

                    User otherUser = userRepository.findById(otherUserId).orElse(null);
                    String userName = otherUser != null
                            ? otherUser.getFirstName() + " " + otherUser.getLastName()
                            : "Unknown User";
                    String photoUrl = otherUser != null ? otherUser.getProfilePhotoUrl() : null;

                    int unreadCount = directMessageRepository.countUnreadFromSender(userId, otherUserId);

                    return ConversationResponse.builder()
                            .userId(otherUserId)
                            .userName(userName)
                            .userProfilePhotoUrl(photoUrl)
                            .lastMessageContent(message.isDeleted() ? DELETED_MESSAGE_CONTENT : message.getContent())
                            .lastMessageSenderId(message.getSenderId())
                            .lastMessageAt(message.getCreatedAt())
                            .unreadCount(unreadCount)
                            .build();
                })
                .sorted(Comparator.comparing(ConversationResponse::getLastMessageAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        int count = directMessageRepository.countUnreadByReceiverId(userId);
        return UnreadCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    private DirectMessageResponse toResponse(DirectMessage message) {
        String senderName = "Unknown User";
        String senderPhotoUrl = null;
        String receiverName = "Unknown User";
        String receiverPhotoUrl = null;

        if (!message.isDeleted()) {
            User sender = userRepository.findById(message.getSenderId()).orElse(null);
            if (sender != null) {
                senderName = sender.getFirstName() + " " + sender.getLastName();
                senderPhotoUrl = sender.getProfilePhotoUrl();
            }

            User receiver = userRepository.findById(message.getReceiverId()).orElse(null);
            if (receiver != null) {
                receiverName = receiver.getFirstName() + " " + receiver.getLastName();
                receiverPhotoUrl = receiver.getProfilePhotoUrl();
            }
        }

        return DirectMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderProfilePhotoUrl(senderPhotoUrl)
                .receiverId(message.getReceiverId())
                .receiverName(receiverName)
                .receiverProfilePhotoUrl(receiverPhotoUrl)
                .content(message.getContent())
                .read(message.isRead())
                .deleted(message.isDeleted())
                .edited(message.getEditedAt() != null)
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
