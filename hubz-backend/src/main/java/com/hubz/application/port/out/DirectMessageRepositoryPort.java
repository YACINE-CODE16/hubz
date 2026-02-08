package com.hubz.application.port.out;

import com.hubz.domain.model.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DirectMessageRepositoryPort {

    DirectMessage save(DirectMessage message);

    Optional<DirectMessage> findById(UUID id);

    /**
     * Find messages between two users, ordered by creation date descending.
     * Includes messages where either user is sender or receiver.
     * Excludes messages deleted by the requesting user.
     */
    Page<DirectMessage> findConversation(UUID userId, UUID otherUserId, Pageable pageable);

    /**
     * Find the latest message for each conversation the user has.
     * Returns a list of the most recent DirectMessage per conversation partner.
     */
    List<DirectMessage> findLatestMessagePerConversation(UUID userId);

    /**
     * Count unread messages received by a user from a specific sender.
     */
    int countUnreadFromSender(UUID receiverId, UUID senderId);

    /**
     * Count all unread messages received by a user.
     */
    int countUnreadByReceiverId(UUID receiverId);

    /**
     * Mark all messages from a specific sender as read for a receiver.
     */
    void markConversationAsRead(UUID receiverId, UUID senderId);
}
