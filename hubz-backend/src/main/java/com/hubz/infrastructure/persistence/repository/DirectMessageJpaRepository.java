package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.DirectMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectMessageJpaRepository extends JpaRepository<DirectMessageEntity, UUID> {

    /**
     * Find messages between two users (bidirectional conversation),
     * ordered by creation date descending.
     */
    @Query("SELECT m FROM DirectMessageEntity m " +
            "WHERE ((m.senderId = :userId AND m.receiverId = :otherUserId) " +
            "   OR (m.senderId = :otherUserId AND m.receiverId = :userId)) " +
            "ORDER BY m.createdAt DESC")
    Page<DirectMessageEntity> findConversation(
            @Param("userId") UUID userId,
            @Param("otherUserId") UUID otherUserId,
            Pageable pageable);

    /**
     * Find all distinct conversation partners for a user, with the latest message.
     * We use a native query to get the latest message per conversation partner.
     */
    @Query(value = "SELECT dm.* FROM direct_messages dm " +
            "INNER JOIN (" +
            "  SELECT " +
            "    CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END AS partner_id, " +
            "    MAX(created_at) AS max_created_at " +
            "  FROM direct_messages " +
            "  WHERE sender_id = :userId OR receiver_id = :userId " +
            "  GROUP BY CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END" +
            ") latest ON (" +
            "  (dm.sender_id = :userId AND dm.receiver_id = latest.partner_id) " +
            "  OR (dm.sender_id = latest.partner_id AND dm.receiver_id = :userId)" +
            ") AND dm.created_at = latest.max_created_at " +
            "ORDER BY dm.created_at DESC",
            nativeQuery = true)
    List<DirectMessageEntity> findLatestMessagePerConversation(@Param("userId") UUID userId);

    /**
     * Count unread messages from a specific sender to a receiver.
     */
    @Query("SELECT COUNT(m) FROM DirectMessageEntity m " +
            "WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND m.read = false")
    int countUnreadFromSender(@Param("receiverId") UUID receiverId, @Param("senderId") UUID senderId);

    /**
     * Count all unread messages for a receiver.
     */
    @Query("SELECT COUNT(m) FROM DirectMessageEntity m WHERE m.receiverId = :receiverId AND m.read = false")
    int countUnreadByReceiverId(@Param("receiverId") UUID receiverId);

    /**
     * Mark all messages from a sender to a receiver as read.
     */
    @Modifying
    @Query("UPDATE DirectMessageEntity m SET m.read = true " +
            "WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND m.read = false")
    void markConversationAsRead(@Param("receiverId") UUID receiverId, @Param("senderId") UUID senderId);
}
