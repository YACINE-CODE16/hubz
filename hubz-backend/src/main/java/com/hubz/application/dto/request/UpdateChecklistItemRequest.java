package com.hubz.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChecklistItemRequest {

    @Size(max = 500, message = "Content must be at most 500 characters")
    private String content;

    private Boolean completed;

    private Integer position;
}
