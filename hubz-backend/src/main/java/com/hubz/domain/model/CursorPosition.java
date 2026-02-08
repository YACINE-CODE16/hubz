package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a user's cursor position in a note during real-time collaboration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPosition {
    private UUID userId;
    private String email;
    private String displayName;
    private String color;
    private Integer position;
    private Integer selectionStart;
    private Integer selectionEnd;
}
