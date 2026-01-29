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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    @Mock
    private TeamRepositoryPort teamRepository;

    @Mock
    private TeamMemberRepositoryPort teamMemberRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TeamService teamService;

    private UUID organizationId;
    private UUID userId;
    private UUID teamId;
    private Team testTeam;
    private TeamMember testTeamMember;
    private User testUser;
    private CreateTeamRequest createRequest;
    private UpdateTeamRequest updateRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        testTeam = Team.builder()
                .id(teamId)
                .name("Test Team")
                .description("Test description")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        testTeamMember = TeamMember.builder()
                .id(UUID.randomUUID())
                .teamId(teamId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateTeamRequest();
        createRequest.setName("Test Team");
        createRequest.setDescription("Test description");

        updateRequest = new UpdateTeamRequest();
        updateRequest.setName("Updated Team");
        updateRequest.setDescription("Updated description");
    }

    @Nested
    @DisplayName("Get Teams By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get teams by organization")
        void shouldGetTeamsByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTeam));
            when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(testTeamMember));

            // When
            List<TeamResponse> teams = teamService.getByOrganization(organizationId, userId);

            // Then
            assertThat(teams).hasSize(1);
            assertThat(teams.get(0).getName()).isEqualTo(testTeam.getName());
            assertThat(teams.get(0).getMemberCount()).isEqualTo(1);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no teams exist")
        void shouldReturnEmptyListWhenNoTeams() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<TeamResponse> teams = teamService.getByOrganization(organizationId, userId);

            // Then
            assertThat(teams).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamService.getByOrganization(organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(teamRepository, never()).findByOrganizationId(any());
        }
    }

    @Nested
    @DisplayName("Get Team Members Tests")
    class GetTeamMembersTests {

        @Test
        @DisplayName("Should successfully get team members")
        void shouldGetTeamMembers() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(testTeamMember));
            when(userRepository.findById(testTeamMember.getUserId())).thenReturn(Optional.of(testUser));

            // When
            List<TeamMemberResponse> members = teamService.getTeamMembers(teamId, userId);

            // Then
            assertThat(members).hasSize(1);
            assertThat(members.get(0).getUserId()).isEqualTo(testUser.getId());
            assertThat(members.get(0).getEmail()).isEqualTo(testUser.getEmail());
            verify(teamRepository).findById(teamId);
            verify(teamMemberRepository).findByTeamId(teamId);
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.getTeamMembers(nonExistentId, userId))
                    .isInstanceOf(TeamNotFoundException.class);
            verify(teamMemberRepository, never()).findByTeamId(any());
        }
    }

    @Nested
    @DisplayName("Create Team Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create team")
        void shouldCreateTeam() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

            // When
            TeamResponse response = teamService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo(testTeam.getName());
            assertThat(response.getDescription()).isEqualTo(testTeam.getDescription());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
            assertThat(response.getMemberCount()).isEqualTo(0);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamRepository).save(any(Team.class));
        }

        @Test
        @DisplayName("Should set createdAt timestamp")
        void shouldSetCreatedAt() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
            when(teamRepository.save(teamCaptor.capture())).thenReturn(testTeam);

            // When
            teamService.create(createRequest, organizationId, userId);

            // Then
            Team savedTeam = teamCaptor.getValue();
            assertThat(savedTeam.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should check organization access before creating")
        void shouldCheckOrganizationAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamService.create(createRequest, organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(teamRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Team Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update team")
        void shouldUpdateTeam() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(teamRepository.save(any(Team.class))).thenReturn(testTeam);
            when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(testTeamMember));

            // When
            TeamResponse response = teamService.update(teamId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(teamRepository).findById(teamId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamRepository).save(any(Team.class));
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(TeamNotFoundException.class);
            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should check organization access before updating")
        void shouldCheckOrganizationAccessBeforeUpdate() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamService.update(teamId, updateRequest, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(teamRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Team Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete team")
        void shouldDeleteTeam() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(teamRepository).delete(testTeam);

            // When
            teamService.delete(teamId, userId);

            // Then
            verify(teamRepository).findById(teamId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamRepository).delete(testTeam);
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.delete(nonExistentId, userId))
                    .isInstanceOf(TeamNotFoundException.class);
            verify(teamRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should check organization access before deleting")
        void shouldCheckOrganizationAccessBeforeDelete() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamService.delete(teamId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(teamRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Add Member Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should successfully add member to team")
        void shouldAddMember() {
            // Given
            UUID newUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(newUserId)).thenReturn(Optional.of(testUser));
            when(teamMemberRepository.existsByTeamIdAndUserId(teamId, newUserId)).thenReturn(false);
            when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(testTeamMember);

            // When
            teamService.addMember(teamId, newUserId, userId);

            // Then
            verify(teamRepository).findById(teamId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamMemberRepository).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UUID newUserId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.addMember(nonExistentId, newUserId, userId))
                    .isInstanceOf(TeamNotFoundException.class);
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.addMember(teamId, nonExistentUserId, userId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when member already exists")
        void shouldThrowExceptionWhenMemberAlreadyExists() {
            // Given
            UUID existingUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(existingUserId)).thenReturn(Optional.of(testUser));
            when(teamMemberRepository.existsByTeamIdAndUserId(teamId, existingUserId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> teamService.addMember(teamId, existingUserId, userId))
                    .isInstanceOf(TeamMemberAlreadyExistsException.class);
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set joinedAt timestamp when adding member")
        void shouldSetJoinedAtTimestamp() {
            // Given
            UUID newUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(newUserId)).thenReturn(Optional.of(testUser));
            when(teamMemberRepository.existsByTeamIdAndUserId(teamId, newUserId)).thenReturn(false);
            ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
            when(teamMemberRepository.save(memberCaptor.capture())).thenReturn(testTeamMember);

            // When
            teamService.addMember(teamId, newUserId, userId);

            // Then
            TeamMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getJoinedAt()).isNotNull();
            assertThat(savedMember.getTeamId()).isEqualTo(teamId);
            assertThat(savedMember.getUserId()).isEqualTo(newUserId);
        }
    }

    @Nested
    @DisplayName("Remove Member Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should successfully remove member from team")
        void shouldRemoveMember() {
            // Given
            UUID memberUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(teamMemberRepository).deleteByTeamIdAndUserId(teamId, memberUserId);

            // When
            teamService.removeMember(teamId, memberUserId, userId);

            // Then
            verify(teamRepository).findById(teamId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(teamMemberRepository).deleteByTeamIdAndUserId(teamId, memberUserId);
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UUID memberUserId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamService.removeMember(nonExistentId, memberUserId, userId))
                    .isInstanceOf(TeamNotFoundException.class);
            verify(teamMemberRepository, never()).deleteByTeamIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should check organization access before removing member")
        void shouldCheckOrganizationAccessBeforeRemove() {
            // Given
            UUID memberUserId = UUID.randomUUID();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamService.removeMember(teamId, memberUserId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(teamMemberRepository, never()).deleteByTeamIdAndUserId(any(), any());
        }
    }
}
