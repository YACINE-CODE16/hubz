package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO containing document preview information.
 * Used to provide metadata about a document along with its preview content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPreviewResponse {
    private UUID id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private boolean previewable;
    private String previewType;  // "image", "pdf", "text", "unsupported"
    private String textContent;  // For text files, contains the actual content
}
