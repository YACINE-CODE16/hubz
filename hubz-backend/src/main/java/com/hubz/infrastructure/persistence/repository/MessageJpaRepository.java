package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

    Page<MessageEntity> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    int countByTeamId(UUID teamId);
}
