package com.hubz.application.dto.response;

import com.hubz.domain.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePhotoUrl;
    private MemberRole role;
    private LocalDateTime joinedAt;
}
