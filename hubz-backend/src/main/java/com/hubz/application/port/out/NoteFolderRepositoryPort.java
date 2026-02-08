package com.hubz.application.port.out;

import com.hubz.domain.model.NoteFolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteFolderRepositoryPort {

    NoteFolder save(NoteFolder folder);

    Optional<NoteFolder> findById(UUID id);

    List<NoteFolder> findByOrganizationId(UUID organizationId);

    List<NoteFolder> findRootFoldersByOrganizationId(UUID organizationId);

    List<NoteFolder> findByParentFolderId(UUID parentFolderId);

    void deleteById(UUID id);

    boolean existsByNameAndOrganizationIdAndParentFolderId(String name, UUID organizationId, UUID parentFolderId);

    long countChildFolders(UUID folderId);
}
