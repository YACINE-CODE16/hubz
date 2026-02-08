package com.hubz.presentation.controller;

import com.hubz.application.dto.response.DocumentPreviewResponse;
import com.hubz.application.dto.response.DocumentVersionResponse;
import com.hubz.application.dto.response.OrganizationDocumentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.FileStorageService;
import com.hubz.application.service.OrganizationDocumentService;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.DocumentVersion;
import com.hubz.domain.model.OrganizationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationDocumentController {

    private final OrganizationDocumentService documentService;
    private final FileStorageService fileStorageService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/{orgId}/documents")
    public ResponseEntity<OrganizationDocumentResponse> uploadDocument(
            @PathVariable UUID orgId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(orgId, file, userId));
    }

    @GetMapping("/{orgId}/documents")
    public ResponseEntity<List<OrganizationDocumentResponse>> getDocuments(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(documentService.getDocuments(orgId, userId));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {
        try {
            UUID userId = resolveUserId(authentication);
            OrganizationDocument document = documentService.getDocument(documentId, userId);

            Path filePath = fileStorageService.getFilePath(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = document.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document: " + e.getMessage(), e);
        }
    }

    /**
     * Get preview metadata for a document.
     * Returns information about whether the document can be previewed
     * and the preview type (image, pdf, text, or unsupported).
     * For text files, also returns the text content.
     */
    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<DocumentPreviewResponse> getDocumentPreview(
            @PathVariable UUID documentId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        DocumentPreviewResponse preview = documentService.getDocumentPreview(documentId, userId);
        return ResponseEntity.ok(preview);
    }

    /**
     * Get the raw file content for inline preview (images, PDFs).
     * Unlike download, this serves the file inline without Content-Disposition: attachment.
     */
    @GetMapping("/documents/{documentId}/preview/content")
    public ResponseEntity<Resource> getDocumentPreviewContent(
            @PathVariable UUID documentId,
            Authentication authentication) {
        try {
            UUID userId = resolveUserId(authentication);
            OrganizationDocument document = documentService.getDocument(documentId, userId);

            Path filePath = fileStorageService.getFilePath(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = document.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document preview: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        documentService.deleteDocument(documentId, userId);
        return ResponseEntity.noContent().build();
    }

    // ================== Document Versioning Endpoints ==================

    /**
     * Get all versions of a document.
     */
    @GetMapping("/documents/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getDocumentVersions(
            @PathVariable UUID documentId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        List<DocumentVersionResponse> versions = documentService.getDocumentVersions(documentId, userId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Upload a new version of an existing document.
     */
    @PostMapping("/documents/{documentId}/versions")
    public ResponseEntity<DocumentVersionResponse> uploadNewVersion(
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        DocumentVersionResponse version = documentService.uploadNewVersion(documentId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    /**
     * Download a specific version of a document.
     */
    @GetMapping("/documents/{documentId}/versions/{versionNumber}/download")
    public ResponseEntity<Resource> downloadDocumentVersion(
            @PathVariable UUID documentId,
            @PathVariable Integer versionNumber,
            Authentication authentication) {
        try {
            UUID userId = resolveUserId(authentication);
            DocumentVersion version = documentService.getDocumentVersionByNumber(documentId, versionNumber, userId);

            Path filePath = fileStorageService.getFilePath(version.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = version.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + version.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document version: " + e.getMessage(), e);
        }
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
