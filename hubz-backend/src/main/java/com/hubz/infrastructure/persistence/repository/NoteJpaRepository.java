package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteJpaRepository extends JpaRepository<NoteEntity, UUID> {

    List<NoteEntity> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

    List<NoteEntity> findByOrganizationIdAndCategoryOrderByUpdatedAtDesc(UUID organizationId, String category);
}
