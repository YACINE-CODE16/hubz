package com.hubz.application.service;

import com.hubz.application.dto.request.CreateOrganizationRequest;
import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.response.MemberResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.CannotChangeOwnerRoleException;
import com.hubz.domain.exception.MemberAlreadyExistsException;
import com.hubz.domain.exception.MemberNotFoundException;
import com.hubz.domain.exception.OrganizationNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepositoryPort organizationRepository;
    private final OrganizationMemberRepositoryPort memberRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID ownerId) {
        Organization org = Organization.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();

        Organization saved = organizationRepository.save(org);

        OrganizationMember ownerMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(saved.getId())
                .userId(ownerId)
                .role(MemberRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();
        memberRepository.save(ownerMember);

        return toResponse(saved);
    }

    public List<OrganizationResponse> getAll() {
        return organizationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OrganizationResponse getById(UUID id) {
        return organizationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
    }

    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request, UUID currentUserId) {
        authorizationService.checkOrganizationAdminAccess(id, currentUserId);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));

        if (request.getName() != null) org.setName(request.getName());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getIcon() != null) org.setIcon(request.getIcon());
        if (request.getColor() != null) org.setColor(request.getColor());
        if (request.getReadme() != null) org.setReadme(request.getReadme());

        return toResponse(organizationRepository.save(org));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        authorizationService.checkOrganizationAdminAccess(id, currentUserId);

        organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        organizationRepository.deleteById(id);
    }

    public List<MemberResponse> getMembers(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        return memberRepository.findByOrganizationId(organizationId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public MemberResponse addMember(UUID organizationId, UUID userId, MemberRole role, UUID currentUserId) {
        authorizationService.checkOrganizationAdminAccess(organizationId, currentUserId);

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new MemberAlreadyExistsException(userId, organizationId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        OrganizationMember member = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        return MemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public void removeMember(UUID organizationId, UUID userId, UUID currentUserId) {
        authorizationService.checkOrganizationAdminAccess(organizationId, currentUserId);
        memberRepository.deleteByOrganizationIdAndUserId(organizationId, userId);
    }

    @Transactional
    public MemberResponse changeMemberRole(UUID organizationId, UUID userId, MemberRole newRole, UUID currentUserId) {
        // Only admins and owners can change roles
        authorizationService.checkOrganizationAdminAccess(organizationId, currentUserId);

        // Find the member
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new MemberNotFoundException(organizationId, userId));

        // Cannot change the owner's role (must transfer ownership instead)
        if (member.getRole() == MemberRole.OWNER) {
            throw new CannotChangeOwnerRoleException();
        }

        // Cannot promote someone to OWNER through role change (must use transfer ownership)
        if (newRole == MemberRole.OWNER) {
            throw new CannotChangeOwnerRoleException();
        }

        // Update the role
        member.setRole(newRole);
        OrganizationMember updated = memberRepository.save(member);

        return toMemberResponse(updated);
    }

    @Transactional
    public void transferOwnership(UUID organizationId, UUID newOwnerId, UUID currentUserId) {
        // Only the current owner can transfer ownership
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (!org.getOwnerId().equals(currentUserId)) {
            throw new CannotChangeOwnerRoleException();
        }

        // Verify new owner is a member
        OrganizationMember newOwnerMember = memberRepository.findByOrganizationIdAndUserId(organizationId, newOwnerId)
                .orElseThrow(() -> new MemberNotFoundException(organizationId, newOwnerId));

        // Find current owner's member record
        OrganizationMember currentOwnerMember = memberRepository.findByOrganizationIdAndUserId(organizationId, currentUserId)
                .orElseThrow(() -> new MemberNotFoundException(organizationId, currentUserId));

        // Demote current owner to admin
        currentOwnerMember.setRole(MemberRole.ADMIN);
        memberRepository.save(currentOwnerMember);

        // Promote new owner
        newOwnerMember.setRole(MemberRole.OWNER);
        memberRepository.save(newOwnerMember);

        // Update organization's owner field
        org.setOwnerId(newOwnerId);
        organizationRepository.save(org);
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .icon(org.getIcon())
                .color(org.getColor())
                .readme(org.getReadme())
                .ownerId(org.getOwnerId())
                .createdAt(org.getCreatedAt())
                .build();
    }

    private MemberResponse toMemberResponse(OrganizationMember member) {
        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new UserNotFoundException(member.getUserId()));
        return MemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
