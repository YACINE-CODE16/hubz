package com.hubz.domain.model;

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
public class OrganizationMember {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private MemberRole role;
    private LocalDateTime joinedAt;
}
