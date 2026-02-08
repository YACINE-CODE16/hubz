package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDocumentResponse {
    private UUID id;
    private UUID organizationId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
    @Builder.Default
    private List<TagResponse> tags = new ArrayList<>();
    private Integer currentVersionNumber;
    private Integer totalVersions;
}
