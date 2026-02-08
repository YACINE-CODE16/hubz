package com.hubz.application.service;

import com.hubz.application.dto.response.DocumentPreviewResponse;
import com.hubz.application.dto.response.DocumentVersionResponse;
import com.hubz.application.dto.response.OrganizationDocumentResponse;
import com.hubz.application.dto.response.TagResponse;
import com.hubz.application.port.out.DocumentVersionRepositoryPort;
import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.model.DocumentVersion;
import com.hubz.domain.model.OrganizationDocument;
import com.hubz.domain.model.Tag;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationDocumentService {

    private final OrganizationDocumentRepositoryPort documentRepository;
    private final DocumentVersionRepositoryPort versionRepository;
    private final TagRepositoryPort tagRepository;
    private final UserRepositoryPort userRepository;
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
            // Delete main document file
            fileStorageService.deleteFile(document.getFilePath());

            // Delete all version files
            List<DocumentVersion> versions = versionRepository.findByDocumentId(documentId);
            for (DocumentVersion version : versions) {
                try {
                    fileStorageService.deleteFile(version.getFilePath());
                } catch (Exception ignored) {
                    // Continue deleting other version files even if one fails
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document file: " + e.getMessage(), e);
        }

        // Delete all versions
        versionRepository.deleteByDocumentId(documentId);

        // Remove all tag associations
        tagRepository.removeAllTagsFromDocument(documentId);

        // Delete metadata
        documentRepository.deleteById(documentId);
    }

    public OrganizationDocument getDocument(UUID documentId, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        return document;
    }

    /**
     * Get preview information for a document.
     * Returns metadata about the document and whether it can be previewed,
     * along with text content for text files.
     *
     * @param documentId the document ID
     * @param userId the requesting user's ID
     * @return DocumentPreviewResponse containing preview information
     */
    public DocumentPreviewResponse getDocumentPreview(UUID documentId, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        String contentType = document.getContentType();
        String previewType = determinePreviewType(contentType, document.getOriginalFileName());
        boolean previewable = !previewType.equals("unsupported");

        String textContent = null;
        if (previewType.equals("text")) {
            textContent = readTextContent(document);
        }

        return DocumentPreviewResponse.builder()
                .id(document.getId())
                .originalFileName(document.getOriginalFileName())
                .contentType(contentType)
                .fileSize(document.getFileSize())
                .previewable(previewable)
                .previewType(previewType)
                .textContent(textContent)
                .build();
    }

    /**
     * Determine the preview type based on content type and file extension.
     *
     * @param contentType the MIME type of the file
     * @param fileName the original file name
     * @return the preview type: "image", "pdf", "text", or "unsupported"
     */
    private String determinePreviewType(String contentType, String fileName) {
        if (contentType == null) {
            return "unsupported";
        }

        // Image types
        Set<String> imageTypes = Set.of(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
        );
        if (imageTypes.contains(contentType)) {
            return "image";
        }

        // PDF
        if (contentType.equals("application/pdf")) {
            return "pdf";
        }

        // Text types
        Set<String> textTypes = Set.of(
                "text/plain", "text/html", "text/css", "text/javascript",
                "text/csv", "text/xml", "text/markdown",
                "application/json", "application/xml", "application/javascript"
        );
        if (textTypes.contains(contentType)) {
            return "text";
        }

        // Check by file extension for text files
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            Set<String> textExtensions = Set.of(
                    ".txt", ".md", ".json", ".xml", ".csv", ".html", ".css", ".js",
                    ".ts", ".java", ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h",
                    ".yml", ".yaml", ".toml", ".ini", ".conf", ".log", ".sh", ".bat"
            );
            for (String ext : textExtensions) {
                if (lowerName.endsWith(ext)) {
                    return "text";
                }
            }
        }

        return "unsupported";
    }

    /**
     * Read the text content of a document file.
     * Limited to 100KB to prevent memory issues.
     *
     * @param document the document to read
     * @return the text content, or an error message if reading fails
     */
    private String readTextContent(OrganizationDocument document) {
        try {
            Path filePath = fileStorageService.getFilePath(document.getFilePath());
            if (!Files.exists(filePath)) {
                return "[File not found]";
            }

            // Limit text preview to 100KB
            long maxSize = 100 * 1024;
            if (document.getFileSize() > maxSize) {
                byte[] bytes = new byte[(int) maxSize];
                try (var inputStream = Files.newInputStream(filePath)) {
                    inputStream.read(bytes);
                }
                return new String(bytes, StandardCharsets.UTF_8) + "\n\n[... Content truncated, file too large for preview ...]";
            }

            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[Error reading file: " + e.getMessage() + "]";
        }
    }

    private OrganizationDocumentResponse toResponse(OrganizationDocument document) {
        List<TagResponse> tags = tagRepository.findTagsByDocumentId(document.getId()).stream()
                .map(this::toTagResponse)
                .toList();

        // Get version info
        List<DocumentVersion> versions = versionRepository.findByDocumentId(document.getId());
        Integer currentVersionNumber = versions.isEmpty() ? 1 :
                versions.stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(1);
        Integer totalVersions = versions.isEmpty() ? 1 : versions.size() + 1; // +1 for the current version

        return OrganizationDocumentResponse.builder()
                .id(document.getId())
                .organizationId(document.getOrganizationId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .uploadedBy(document.getUploadedBy())
                .uploadedAt(document.getUploadedAt())
                .tags(tags)
                .currentVersionNumber(currentVersionNumber)
                .totalVersions(totalVersions)
                .build();
    }

    private TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .organizationId(tag.getOrganizationId())
                .createdAt(tag.getCreatedAt())
                .build();
    }

    // ================== Document Versioning Methods ==================

    /**
     * Upload a new version of an existing document.
     * The previous version is stored in the version history.
     *
     * @param documentId the document ID
     * @param file the new version file
     * @param uploadedBy the user uploading the new version
     * @return DocumentVersionResponse containing the new version information
     */
    @Transactional
    public DocumentVersionResponse uploadNewVersion(UUID documentId, MultipartFile file, UUID uploadedBy) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), uploadedBy);

        try {
            // Get the next version number
            Integer maxVersion = versionRepository.findMaxVersionNumberByDocumentId(documentId).orElse(0);
            int newVersionNumber = maxVersion + 1;

            // If this is the first version upload, save the current document as version 1
            if (maxVersion == 0) {
                DocumentVersion originalVersion = DocumentVersion.builder()
                        .id(UUID.randomUUID())
                        .documentId(documentId)
                        .versionNumber(1)
                        .fileName(document.getFileName())
                        .originalFileName(document.getOriginalFileName())
                        .filePath(document.getFilePath())
                        .fileSize(document.getFileSize())
                        .contentType(document.getContentType())
                        .uploadedBy(document.getUploadedBy())
                        .uploadedAt(document.getUploadedAt())
                        .build();
                versionRepository.save(originalVersion);
                newVersionNumber = 2;
            }

            // Store the new file
            String newFileName = fileStorageService.storeFile(file, document.getOrganizationId());

            // Create new version record
            DocumentVersion newVersion = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(newVersionNumber)
                    .fileName(newFileName)
                    .originalFileName(file.getOriginalFilename())
                    .filePath(newFileName)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            DocumentVersion savedVersion = versionRepository.save(newVersion);

            // Update the main document to point to the new version
            document.setFileName(newFileName);
            document.setOriginalFileName(file.getOriginalFilename());
            document.setFilePath(newFileName);
            document.setFileSize(file.getSize());
            document.setContentType(file.getContentType());
            document.setUploadedBy(uploadedBy);
            document.setUploadedAt(LocalDateTime.now());
            documentRepository.save(document);

            return toVersionResponse(savedVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload new version: " + e.getMessage(), e);
        }
    }

    /**
     * Get all versions of a document.
     *
     * @param documentId the document ID
     * @param userId the requesting user's ID
     * @return list of DocumentVersionResponse ordered by version number descending
     */
    public List<DocumentVersionResponse> getDocumentVersions(UUID documentId, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        List<DocumentVersion> versions = versionRepository.findByDocumentId(documentId);

        // If no versions exist yet, return the current document as version 1
        if (versions.isEmpty()) {
            DocumentVersionResponse currentVersion = DocumentVersionResponse.builder()
                    .id(document.getId())
                    .documentId(documentId)
                    .versionNumber(1)
                    .fileName(document.getFileName())
                    .originalFileName(document.getOriginalFileName())
                    .fileSize(document.getFileSize())
                    .contentType(document.getContentType())
                    .uploadedBy(document.getUploadedBy())
                    .uploadedByName(getUserFullName(document.getUploadedBy()))
                    .uploadedAt(document.getUploadedAt())
                    .build();
            return List.of(currentVersion);
        }

        return versions.stream()
                .map(this::toVersionResponse)
                .toList();
    }

    /**
     * Get a specific version of a document.
     *
     * @param versionId the version ID
     * @param userId the requesting user's ID
     * @return the DocumentVersion
     */
    public DocumentVersion getDocumentVersion(UUID versionId, UUID userId) {
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Document version not found"));

        OrganizationDocument document = documentRepository.findById(version.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        return version;
    }

    /**
     * Download a specific version of a document.
     *
     * @param documentId the document ID
     * @param versionNumber the version number to download
     * @param userId the requesting user's ID
     * @return the DocumentVersion containing file path info
     */
    public DocumentVersion getDocumentVersionByNumber(UUID documentId, Integer versionNumber, UUID userId) {
        OrganizationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        authorizationService.checkOrganizationAccess(document.getOrganizationId(), userId);

        List<DocumentVersion> versions = versionRepository.findByDocumentId(documentId);

        // If requesting version 1 and no versions exist, return document info as version
        if (versionNumber == 1 && versions.isEmpty()) {
            return DocumentVersion.builder()
                    .id(document.getId())
                    .documentId(documentId)
                    .versionNumber(1)
                    .fileName(document.getFileName())
                    .originalFileName(document.getOriginalFileName())
                    .filePath(document.getFilePath())
                    .fileSize(document.getFileSize())
                    .contentType(document.getContentType())
                    .uploadedBy(document.getUploadedBy())
                    .uploadedAt(document.getUploadedAt())
                    .build();
        }

        return versions.stream()
                .filter(v -> v.getVersionNumber().equals(versionNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found"));
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        return DocumentVersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocumentId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .originalFileName(version.getOriginalFileName())
                .fileSize(version.getFileSize())
                .contentType(version.getContentType())
                .uploadedBy(version.getUploadedBy())
                .uploadedByName(getUserFullName(version.getUploadedBy()))
                .uploadedAt(version.getUploadedAt())
                .build();
    }

    private String getUserFullName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Unknown User");
    }
}
