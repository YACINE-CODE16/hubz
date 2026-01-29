package com.hubz.application.service;

import com.hubz.application.dto.request.CreateInvitationRequest;
import com.hubz.application.dto.response.InvitationResponse;
import com.hubz.application.port.out.OrganizationInvitationRepositoryPort;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationInvitation;
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
public class OrganizationInvitationService {

    private final OrganizationInvitationRepositoryPort invitationRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final OrganizationMemberRepositoryPort memberRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;
    private final EmailService emailService;

    private static final int INVITATION_EXPIRY_DAYS = 7;

    @Transactional
    public InvitationResponse createInvitation(
            UUID organizationId,
            CreateInvitationRequest request,
            UUID createdBy) {

        // Check if user has admin access
        authorizationService.checkOrganizationAdminAccess(organizationId, createdBy);

        // Check if organization exists
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Check if user is already a member
        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, createdBy)) {
            // Check if user exists
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                if (memberRepository.existsByOrganizationIdAndUserId(organizationId, user.getId())) {
                    throw new RuntimeException("User is already a member of this organization");
                }
            });
        }

        // Check if there's already a pending invitation
        invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, request.getEmail())
                .ifPresent(invitation -> {
                    throw new RuntimeException("An invitation has already been sent to this email");
                });

        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Create invitation
        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .email(request.getEmail())
                .role(request.getRole())
                .token(token)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS))
                .used(false)
                .build();

        OrganizationInvitation saved = invitationRepository.save(invitation);

        // Send invitation email
        try {
            emailService.sendInvitationEmail(
                    request.getEmail(),
                    org.getName(),
                    token,
                    request.getRole().name()
            );
        } catch (Exception e) {
            // Log error but don't fail the invitation creation
            // The user can still copy the link manually
            System.err.println("Failed to send invitation email: " + e.getMessage());
        }

        return toResponse(saved, org.getName());
    }

    public List<InvitationResponse> getInvitations(UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAdminAccess(organizationId, userId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        return invitationRepository.findByOrganizationId(organizationId).stream()
                .map(inv -> toResponse(inv, org.getName()))
                .toList();
    }

    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        // Find invitation
        OrganizationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        // Check if already used
        if (invitation.getUsed()) {
            throw new RuntimeException("This invitation has already been used");
        }

        // Check if expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This invitation has expired");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if email matches (optional - can allow any logged-in user)
        // Uncomment if you want strict email matching
        // if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
        //     throw new RuntimeException("This invitation was sent to a different email address");
        // }

        // Check if user is already a member
        if (memberRepository.existsByOrganizationIdAndUserId(invitation.getOrganizationId(), userId)) {
            throw new RuntimeException("You are already a member of this organization");
        }

        // Add user as member
        OrganizationMember member = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(invitation.getOrganizationId())
                .userId(userId)
                .role(invitation.getRole())
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        // Mark invitation as used
        invitation.setUsed(true);
        invitation.setAcceptedBy(userId);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    public InvitationResponse getInvitationByToken(String token) {
        OrganizationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        Organization org = organizationRepository.findById(invitation.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        return toResponse(invitation, org.getName());
    }

    @Transactional
    public void deleteInvitation(UUID invitationId, UUID userId) {
        OrganizationInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        authorizationService.checkOrganizationAdminAccess(invitation.getOrganizationId(), userId);

        invitationRepository.deleteById(invitationId);
    }

    private InvitationResponse toResponse(OrganizationInvitation invitation, String organizationName) {
        String invitationUrl = "/join/" + invitation.getToken();

        return InvitationResponse.builder()
                .id(invitation.getId())
                .organizationId(invitation.getOrganizationId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .token(invitation.getToken())
                .invitationUrl(invitationUrl)
                .createdBy(invitation.getCreatedBy())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .used(invitation.getUsed())
                .acceptedBy(invitation.getAcceptedBy())
                .acceptedAt(invitation.getAcceptedAt())
                .build();
    }
}
