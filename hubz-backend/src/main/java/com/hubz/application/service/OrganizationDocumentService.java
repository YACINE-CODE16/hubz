package com.hubz.application.service;

import com.hubz.application.dto.response.OrganizationDocumentResponse;
import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.domain.model.OrganizationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationDocumentService {

    private final OrganizationDocumentRepositoryPort documentRepository;
    private final FileStorageService fileStorageService;
    private final AuthorizationService authorizationService;

    @Transactional
    public OrganizationDocumentResponse uploadDocument(
            UUID organizationId,
            MultipartFile file,
            UUID uploadedBy) {

        // Check authorization
        authorizationService.checkOrganizationAccess(organizationId, uploadedBy);

        try {
            // Store file using organizationId as the directory
            String fileName = fileStorageService.storeFile(file, organizationId);

            // Save metadata
            OrganizationDocument document = OrganizationDocument.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .filePath(fileName)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            OrganizationDocument saved = documentRepository.save(document);
            return toResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    public List<OrganizationDocumentResponse> getDocuments(UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);

        return documentRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        try {
            // Delete file
            fileStorageService.deleteFile(document.getFilePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document file: " + e.getMessage(), e);
        }

        // Delete metadata
        documentRepository.deleteById(documentId);
    }

    public OrganizationDocument getDocument(UUID documentId, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        return document;
    }

    private OrganizationDocumentResponse toResponse(OrganizationDocument document) {
        return OrganizationDocumentResponse.builder()
                .id(document.getId())
                .organizationId(document.getOrganizationId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .uploadedBy(document.getUploadedBy())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
