package com.hubz.application.service;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final OrganizationMemberRepositoryPort memberRepository;
    private final TaskRepositoryPort taskRepository;

    private static final Set<MemberRole> ADMIN_ROLES = Set.of(MemberRole.OWNER, MemberRole.ADMIN);

    public boolean isOrganizationMember(UUID orgId, UUID userId) {
        return memberRepository.findByOrganizationIdAndUserId(orgId, userId).isPresent();
    }

    public boolean isOrganizationAdmin(UUID orgId, UUID userId) {
        return memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .map(member -> ADMIN_ROLES.contains(member.getRole()))
                .orElse(false);
    }

    public boolean isTaskOwnerOrAssignee(UUID taskId, UUID userId) {
        return taskRepository.findById(taskId)
                .map(task -> userId.equals(task.getCreatorId()) || userId.equals(task.getAssigneeId()))
                .orElse(false);
    }

    public void checkOrganizationAccess(UUID orgId, UUID userId) {
        if (!isOrganizationMember(orgId, userId)) {
            throw AccessDeniedException.notMember();
        }
    }

    public void checkOrganizationAdminAccess(UUID orgId, UUID userId) {
        if (!isOrganizationAdmin(orgId, userId)) {
            throw AccessDeniedException.notAdmin();
        }
    }
}
