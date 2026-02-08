package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.DirectMessageRepositoryPort;
import com.hubz.domain.model.DirectMessage;
import com.hubz.infrastructure.persistence.mapper.DirectMessageMapper;
import com.hubz.infrastructure.persistence.repository.DirectMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DirectMessageRepositoryAdapter implements DirectMessageRepositoryPort {

    private final DirectMessageJpaRepository jpaRepository;
    private final DirectMessageMapper mapper;

    @Override
    public DirectMessage save(DirectMessage message) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(message)));
    }

    @Override
    public Optional<DirectMessage> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<DirectMessage> findConversation(UUID userId, UUID otherUserId, Pageable pageable) {
        return jpaRepository.findConversation(userId, otherUserId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<DirectMessage> findLatestMessagePerConversation(UUID userId) {
        return jpaRepository.findLatestMessagePerConversation(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countUnreadFromSender(UUID receiverId, UUID senderId) {
        return jpaRepository.countUnreadFromSender(receiverId, senderId);
    }

    @Override
    public int countUnreadByReceiverId(UUID receiverId) {
        return jpaRepository.countUnreadByReceiverId(receiverId);
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID receiverId, UUID senderId) {
        jpaRepository.markConversationAsRead(receiverId, senderId);
    }
}
