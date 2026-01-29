package com.hubz.domain.model;

import com.hubz.domain.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvitation {
    private UUID id;
    private UUID organizationId;
    private String email;
    private MemberRole role;
    private String token;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean used;
    private UUID acceptedBy;
    private LocalDateTime acceptedAt;
}
