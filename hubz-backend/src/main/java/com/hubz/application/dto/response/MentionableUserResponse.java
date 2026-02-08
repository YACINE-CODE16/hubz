package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a user that can be mentioned in comments.
 * Used for the autocomplete feature in the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionableUserResponse {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String mentionName;
    private String profilePhotoUrl;
}
