package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteFolderRequest {

    @NotBlank(message = "Folder name is required")
    @Size(max = 100, message = "Folder name must be at most 100 characters")
    private String name;

    private UUID parentFolderId;
}
