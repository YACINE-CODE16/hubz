package com.hubz.application.port.out;

import com.hubz.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepositoryPort {
    Message save(Message message);

    Optional<Message> findById(UUID id);

    Page<Message> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    void delete(Message message);

    int countByTeamId(UUID teamId);
}
