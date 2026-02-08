package com.hubz.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCursorRequest {
    private UUID noteId;
    private Integer position;
    private Integer selectionStart;
    private Integer selectionEnd;
}
