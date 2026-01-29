package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTeamRequest;
import com.hubz.application.dto.request.UpdateTeamRequest;
import com.hubz.application.dto.response.TeamMemberResponse;
import com.hubz.application.dto.response.TeamResponse;
import com.hubz.application.port.out.TeamMemberRepositoryPort;
import com.hubz.application.port.out.TeamRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.TeamMemberAlreadyExistsException;
import com.hubz.domain.exception.TeamNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Team;
import com.hubz.domain.model.TeamMember;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepositoryPort teamRepository;
    private final TeamMemberRepositoryPort teamMemberRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<TeamResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return teamRepository.findByOrganizationId(organizationId).stream()
                .map(team -> {
                    int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
                    return toResponse(team, memberCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamResponse create(CreateTeamRequest request, UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        Team saved = teamRepository.save(team);
        return toResponse(saved, 0);
    }

    @Transactional
    public TeamResponse update(UUID id, UpdateTeamRequest request, UUID currentUserId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        team.setName(request.getName());
        team.setDescription(request.getDescription());

        Team updated = teamRepository.save(team);
        int memberCount = teamMemberRepository.findByTeamId(id).size();
        return toResponse(updated, memberCount);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        teamRepository.delete(team);
    }

    @Transactional
    public void addMember(UUID teamId, UUID userId, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        // Check if user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Check if already a member
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamMemberAlreadyExistsException("User is already a member of this team");
        }

        TeamMember teamMember = TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        teamMemberRepository.save(teamMember);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId, UUID currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        authorizationService.checkOrganizationAccess(team.getOrganizationId(), currentUserId);

        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    private TeamResponse toResponse(Team team, int memberCount) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .organizationId(team.getOrganizationId())
                .createdAt(team.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }

    private TeamMemberResponse toMemberResponse(TeamMember teamMember) {
        User user = userRepository.findById(teamMember.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return TeamMemberResponse.builder()
                .id(teamMember.getId())
                .teamId(teamMember.getTeamId())
                .userId(teamMember.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .joinedAt(teamMember.getJoinedAt())
                .build();
    }
}
