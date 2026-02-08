package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.MessageRepositoryPort;
import com.hubz.domain.model.Message;
import com.hubz.infrastructure.persistence.entity.MessageEntity;
import com.hubz.infrastructure.persistence.mapper.MessageMapper;
import com.hubz.infrastructure.persistence.repository.MessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepositoryPort {

    private final MessageJpaRepository jpaRepository;
    private final MessageMapper mapper;

    @Override
    public Message save(Message message) {
        MessageEntity entity = mapper.toEntity(message);
        MessageEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Message> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable) {
        return jpaRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(Message message) {
        jpaRepository.deleteById(message.getId());
    }

    @Override
    public int countByTeamId(UUID teamId) {
        return jpaRepository.countByTeamId(teamId);
    }
}
