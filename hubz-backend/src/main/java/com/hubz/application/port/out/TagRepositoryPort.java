package com.hubz.application.port.out;

import com.hubz.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TagRepositoryPort {

    Tag save(Tag tag);

    Optional<Tag> findById(UUID id);

    List<Tag> findByOrganizationId(UUID organizationId);

    List<Tag> findByIds(Set<UUID> ids);

    void deleteById(UUID id);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    // Task-Tag relationship methods
    void addTagToTask(UUID taskId, UUID tagId);

    void removeTagFromTask(UUID taskId, UUID tagId);

    void removeAllTagsFromTask(UUID taskId);

    List<Tag> findTagsByTaskId(UUID taskId);

    List<UUID> findTaskIdsByTagId(UUID tagId);

    // Document-Tag relationship methods
    void addTagToDocument(UUID documentId, UUID tagId);

    void removeTagFromDocument(UUID documentId, UUID tagId);

    void removeAllTagsFromDocument(UUID documentId);

    List<Tag> findTagsByDocumentId(UUID documentId);

    List<UUID> findDocumentIdsByTagId(UUID tagId);
}
