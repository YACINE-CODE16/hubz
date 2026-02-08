package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.domain.model.Tag;
import com.hubz.infrastructure.persistence.entity.DocumentTagEntity;
import com.hubz.infrastructure.persistence.entity.TaskTagEntity;
import com.hubz.infrastructure.persistence.mapper.TagMapper;
import com.hubz.infrastructure.persistence.repository.DocumentTagJpaRepository;
import com.hubz.infrastructure.persistence.repository.TagJpaRepository;
import com.hubz.infrastructure.persistence.repository.TaskTagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepositoryPort {

    private final TagJpaRepository tagJpaRepository;
    private final TaskTagJpaRepository taskTagJpaRepository;
    private final DocumentTagJpaRepository documentTagJpaRepository;
    private final TagMapper mapper;

    @Override
    public Tag save(Tag tag) {
        var entity = mapper.toEntity(tag);
        var saved = tagJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return tagJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tag> findByOrganizationId(UUID organizationId) {
        return tagJpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tag> findByIds(Set<UUID> ids) {
        return tagJpaRepository.findByIdIn(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        // First delete all task-tag associations
        taskTagJpaRepository.deleteAllByTagId(id);
        // Delete all document-tag associations
        documentTagJpaRepository.deleteAllByTagId(id);
        // Then delete the tag
        tagJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndOrganizationId(String name, UUID organizationId) {
        return tagJpaRepository.existsByNameAndOrganizationId(name, organizationId);
    }

    @Override
    public void addTagToTask(UUID taskId, UUID tagId) {
        if (!taskTagJpaRepository.existsByTaskIdAndTagId(taskId, tagId)) {
            TaskTagEntity entity = TaskTagEntity.builder()
                    .id(UUID.randomUUID())
                    .taskId(taskId)
                    .tagId(tagId)
                    .createdAt(LocalDateTime.now())
                    .build();
            taskTagJpaRepository.save(entity);
        }
    }

    @Override
    public void removeTagFromTask(UUID taskId, UUID tagId) {
        taskTagJpaRepository.findByTaskIdAndTagId(taskId, tagId)
                .ifPresent(taskTagJpaRepository::delete);
    }

    @Override
    public void removeAllTagsFromTask(UUID taskId) {
        taskTagJpaRepository.deleteAllByTaskId(taskId);
    }

    @Override
    public List<Tag> findTagsByTaskId(UUID taskId) {
        return tagJpaRepository.findByTaskId(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findTaskIdsByTagId(UUID tagId) {
        return taskTagJpaRepository.findByTagId(tagId).stream()
                .map(TaskTagEntity::getTaskId)
                .toList();
    }

    // Document-Tag relationship methods

    @Override
    public void addTagToDocument(UUID documentId, UUID tagId) {
        if (!documentTagJpaRepository.existsByDocumentIdAndTagId(documentId, tagId)) {
            DocumentTagEntity entity = DocumentTagEntity.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .tagId(tagId)
                    .createdAt(LocalDateTime.now())
                    .build();
            documentTagJpaRepository.save(entity);
        }
    }

    @Override
    public void removeTagFromDocument(UUID documentId, UUID tagId) {
        documentTagJpaRepository.findByDocumentIdAndTagId(documentId, tagId)
                .ifPresent(documentTagJpaRepository::delete);
    }

    @Override
    public void removeAllTagsFromDocument(UUID documentId) {
        documentTagJpaRepository.deleteAllByDocumentId(documentId);
    }

    @Override
    public List<Tag> findTagsByDocumentId(UUID documentId) {
        return tagJpaRepository.findByDocumentId(documentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findDocumentIdsByTagId(UUID tagId) {
        return documentTagJpaRepository.findByTagId(tagId).stream()
                .map(DocumentTagEntity::getDocumentId)
                .toList();
    }
}
