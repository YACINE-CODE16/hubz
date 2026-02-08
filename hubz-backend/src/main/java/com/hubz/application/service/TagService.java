package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTagRequest;
import com.hubz.application.dto.request.UpdateTagRequest;
import com.hubz.application.dto.response.TagResponse;
import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.exception.TagNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.OrganizationDocument;
import com.hubz.domain.model.Tag;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepositoryPort tagRepository;
    private final TaskRepositoryPort taskRepository;
    private final OrganizationDocumentRepositoryPort documentRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public TagResponse create(CreateTagRequest request, UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        Tag tag = Tag.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .color(request.getColor())
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(tagRepository.save(tag));
    }

    public List<TagResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return tagRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TagResponse getById(UUID id, UUID currentUserId) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        return toResponse(tag);
    }

    @Transactional
    public TagResponse update(UUID id, UpdateTagRequest request, UUID currentUserId) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        if (request.getName() != null) {
            tag.setName(request.getName());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        tagRepository.deleteById(id);
    }

    // Task-Tag operations

    @Transactional
    public void addTagToTask(UUID taskId, UUID tagId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));

        // Verify both belong to the same organization
        if (!task.getOrganizationId().equals(tag.getOrganizationId())) {
            throw new IllegalArgumentException("Tag and task must belong to the same organization");
        }

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        tagRepository.addTagToTask(taskId, tagId);
    }

    @Transactional
    public void removeTagFromTask(UUID taskId, UUID tagId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        tagRepository.removeTagFromTask(taskId, tagId);
    }

    @Transactional
    public void setTaskTags(UUID taskId, Set<UUID> tagIds, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        // Verify all tags belong to the same organization
        if (!tagIds.isEmpty()) {
            List<Tag> tags = tagRepository.findByIds(tagIds);
            for (Tag tag : tags) {
                if (!tag.getOrganizationId().equals(task.getOrganizationId())) {
                    throw new IllegalArgumentException("All tags must belong to the same organization as the task");
                }
            }
        }

        // Remove all existing tags and add new ones
        tagRepository.removeAllTagsFromTask(taskId);
        for (UUID tagId : tagIds) {
            tagRepository.addTagToTask(taskId, tagId);
        }
    }

    public List<TagResponse> getTagsByTask(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        return tagRepository.findTagsByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Document-Tag operations

    @Transactional
    public void addTagToDocument(UUID documentId, UUID tagId, UUID currentUserId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));

        // Verify both belong to the same organization
        if (!document.getOrganizationId().equals(tag.getOrganizationId())) {
            throw new IllegalArgumentException("Tag and document must belong to the same organization");
        }

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), currentUserId);

        tagRepository.addTagToDocument(documentId, tagId);
    }

    @Transactional
    public void removeTagFromDocument(UUID documentId, UUID tagId, UUID currentUserId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), currentUserId);

        tagRepository.removeTagFromDocument(documentId, tagId);
    }

    @Transactional
    public void setDocumentTags(UUID documentId, Set<UUID> tagIds, UUID currentUserId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), currentUserId);

        // Verify all tags belong to the same organization
        if (!tagIds.isEmpty()) {
            List<Tag> tags = tagRepository.findByIds(tagIds);
            for (Tag tag : tags) {
                if (!tag.getOrganizationId().equals(document.getOrganizationId())) {
                    throw new IllegalArgumentException("All tags must belong to the same organization as the document");
                }
            }
        }

        // Remove all existing tags and add new ones
        tagRepository.removeAllTagsFromDocument(documentId);
        for (UUID tagId : tagIds) {
            tagRepository.addTagToDocument(documentId, tagId);
        }
    }

    public List<TagResponse> getTagsByDocument(UUID documentId, UUID currentUserId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), currentUserId);

        return tagRepository.findTagsByDocumentId(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .organizationId(tag.getOrganizationId())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
