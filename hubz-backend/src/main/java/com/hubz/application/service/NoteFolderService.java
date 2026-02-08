package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteFolderRequest;
import com.hubz.application.dto.request.UpdateNoteFolderRequest;
import com.hubz.application.dto.response.NoteFolderResponse;
import com.hubz.application.port.out.NoteFolderRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteFolderNotFoundException;
import com.hubz.domain.model.NoteFolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteFolderService {

    private final NoteFolderRepositoryPort folderRepository;
    private final NoteRepositoryPort noteRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public NoteFolderResponse create(CreateNoteFolderRequest request, UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        // Validate parent folder if provided
        if (request.getParentFolderId() != null) {
            NoteFolder parent = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new NoteFolderNotFoundException(request.getParentFolderId()));

            if (!parent.getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("Parent folder does not belong to this organization");
            }
        }

        NoteFolder folder = NoteFolder.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .parentFolderId(request.getParentFolderId())
                .organizationId(organizationId)
                .createdById(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(folderRepository.save(folder), 0);
    }

    @Transactional(readOnly = true)
    public List<NoteFolderResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<NoteFolder> allFolders = folderRepository.findByOrganizationId(organizationId);

        // Build tree structure
        return buildFolderTree(allFolders, organizationId);
    }

    @Transactional(readOnly = true)
    public List<NoteFolderResponse> getFlatList(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return folderRepository.findByOrganizationId(organizationId).stream()
                .map(folder -> toResponse(folder, countNotesInFolder(folder.getId(), organizationId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteFolderResponse getById(UUID id, UUID currentUserId) {
        NoteFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new NoteFolderNotFoundException(id));

        authorizationService.checkOrganizationAccess(folder.getOrganizationId(), currentUserId);

        return toResponse(folder, countNotesInFolder(folder.getId(), folder.getOrganizationId()));
    }

    @Transactional
    public NoteFolderResponse update(UUID id, UpdateNoteFolderRequest request, UUID currentUserId) {
        NoteFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new NoteFolderNotFoundException(id));

        authorizationService.checkOrganizationAccess(folder.getOrganizationId(), currentUserId);

        // Validate parent folder change if provided
        if (request.getParentFolderId() != null) {
            // Prevent circular reference
            if (request.getParentFolderId().equals(id)) {
                throw new IllegalArgumentException("A folder cannot be its own parent");
            }

            // Check if new parent is a descendant of this folder
            if (isDescendant(request.getParentFolderId(), id)) {
                throw new IllegalArgumentException("Cannot move a folder into its own descendant");
            }

            NoteFolder parent = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new NoteFolderNotFoundException(request.getParentFolderId()));

            if (!parent.getOrganizationId().equals(folder.getOrganizationId())) {
                throw new IllegalArgumentException("Parent folder does not belong to this organization");
            }

            folder.setParentFolderId(request.getParentFolderId());
        } else if (request.getParentFolderId() == null && request.getName() == null) {
            // If explicitly setting parent to null (root level)
            folder.setParentFolderId(null);
        }

        if (request.getName() != null) {
            folder.setName(request.getName());
        }

        folder.setUpdatedAt(LocalDateTime.now());

        return toResponse(folderRepository.save(folder), countNotesInFolder(folder.getId(), folder.getOrganizationId()));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        NoteFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new NoteFolderNotFoundException(id));

        authorizationService.checkOrganizationAccess(folder.getOrganizationId(), currentUserId);

        // Check if folder has children
        long childCount = folderRepository.countChildFolders(id);
        if (childCount > 0) {
            throw new IllegalArgumentException("Cannot delete folder with subfolders. Delete subfolders first.");
        }

        // Check if folder has notes - don't delete if it has notes
        long noteCount = countNotesInFolder(id, folder.getOrganizationId());
        if (noteCount > 0) {
            throw new IllegalArgumentException("Cannot delete folder with notes. Move or delete notes first.");
        }

        folderRepository.deleteById(id);
    }

    private boolean isDescendant(UUID potentialDescendantId, UUID ancestorId) {
        NoteFolder current = folderRepository.findById(potentialDescendantId).orElse(null);
        while (current != null && current.getParentFolderId() != null) {
            if (current.getParentFolderId().equals(ancestorId)) {
                return true;
            }
            current = folderRepository.findById(current.getParentFolderId()).orElse(null);
        }
        return false;
    }

    private List<NoteFolderResponse> buildFolderTree(List<NoteFolder> allFolders, UUID organizationId) {
        Map<UUID, List<NoteFolder>> childrenMap = allFolders.stream()
                .filter(f -> f.getParentFolderId() != null)
                .collect(Collectors.groupingBy(NoteFolder::getParentFolderId));

        // Get root folders (those without parent)
        List<NoteFolder> rootFolders = allFolders.stream()
                .filter(f -> f.getParentFolderId() == null)
                .toList();

        return rootFolders.stream()
                .map(folder -> buildFolderResponseWithChildren(folder, childrenMap, organizationId))
                .toList();
    }

    private NoteFolderResponse buildFolderResponseWithChildren(
            NoteFolder folder,
            Map<UUID, List<NoteFolder>> childrenMap,
            UUID organizationId) {

        List<NoteFolder> children = childrenMap.getOrDefault(folder.getId(), new ArrayList<>());
        List<NoteFolderResponse> childResponses = children.stream()
                .map(child -> buildFolderResponseWithChildren(child, childrenMap, organizationId))
                .toList();

        NoteFolderResponse response = toResponse(folder, countNotesInFolder(folder.getId(), organizationId));
        response.setChildren(childResponses);
        return response;
    }

    private long countNotesInFolder(UUID folderId, UUID organizationId) {
        return noteRepository.findByOrganizationId(organizationId).stream()
                .filter(note -> folderId.equals(note.getFolderId()))
                .count();
    }

    private NoteFolderResponse toResponse(NoteFolder folder, long noteCount) {
        return NoteFolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentFolderId(folder.getParentFolderId())
                .organizationId(folder.getOrganizationId())
                .createdById(folder.getCreatedById())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .noteCount(noteCount)
                .children(new ArrayList<>())
                .build();
    }
}
