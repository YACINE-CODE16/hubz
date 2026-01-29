package com.hubz.presentation.controller;

import com.hubz.application.dto.response.OrganizationDocumentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.FileStorageService;
import com.hubz.application.service.OrganizationDocumentService;
import com.hubz.domain.exception.UserNotFoundException;
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

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        documentService.deleteDocument(documentId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
