package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user actively collaborating on a note.
 * This is a transient model (not persisted) used for real-time collaboration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCollaborator {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePhotoUrl;
    private String color;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;

    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email;
    }

    public String getInitials() {
        if (firstName != null && lastName != null) {
            return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
        }
        if (email != null && !email.isEmpty()) {
            return email.substring(0, 1).toUpperCase();
        }
        return "?";
    }
}
