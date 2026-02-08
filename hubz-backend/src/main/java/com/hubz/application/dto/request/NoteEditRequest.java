package com.hubz.application.dto.request;

import com.hubz.domain.model.NoteEditOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEditRequest {
    private UUID noteId;
    private NoteEditOperation.EditType type;
    private String title;
    private String content;
    private Long baseVersion;
}
