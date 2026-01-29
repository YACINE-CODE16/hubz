package com.hubz.application.service;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Unit Tests")
class AuthorizationServiceTest {

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private UUID organizationId;
    private UUID userId;
    private UUID taskId;
    private OrganizationMember ownerMember;
    private OrganizationMember adminMember;
    private OrganizationMember regularMember;
    private OrganizationMember viewerMember;
    private Task testTask;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        ownerMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        adminMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        regularMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        viewerMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.VIEWER)
                .joinedAt(LocalDateTime.now())
                .build();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .dueDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Organization Member Check Tests")
    class IsOrganizationMemberTests {

        @Test
        @DisplayName("Should return true when user is member")
        void shouldReturnTrueWhenUserIsMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(regularMember));

            // When
            boolean isMember = authorizationService.isOrganizationMember(organizationId, userId);

            // Then
            assertThat(isMember).isTrue();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.empty());

            // When
            boolean isMember = authorizationService.isOrganizationMember(organizationId, userId);

            // Then
            assertThat(isMember).isFalse();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }
    }

    @Nested
    @DisplayName("Organization Admin Check Tests")
    class IsOrganizationAdminTests {

        @Test
        @DisplayName("Should return true when user is owner")
        void shouldReturnTrueWhenUserIsOwner() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(ownerMember));

            // When
            boolean isAdmin = authorizationService.isOrganizationAdmin(organizationId, userId);

            // Then
            assertThat(isAdmin).isTrue();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should return true when user is admin")
        void shouldReturnTrueWhenUserIsAdmin() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(adminMember));

            // When
            boolean isAdmin = authorizationService.isOrganizationAdmin(organizationId, userId);

            // Then
            assertThat(isAdmin).isTrue();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should return false when user is regular member")
        void shouldReturnFalseWhenUserIsRegularMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(regularMember));

            // When
            boolean isAdmin = authorizationService.isOrganizationAdmin(organizationId, userId);

            // Then
            assertThat(isAdmin).isFalse();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should return false when user is viewer")
        void shouldReturnFalseWhenUserIsViewer() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(viewerMember));

            // When
            boolean isAdmin = authorizationService.isOrganizationAdmin(organizationId, userId);

            // Then
            assertThat(isAdmin).isFalse();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.empty());

            // When
            boolean isAdmin = authorizationService.isOrganizationAdmin(organizationId, userId);

            // Then
            assertThat(isAdmin).isFalse();
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }
    }

    @Nested
    @DisplayName("Task Owner Or Assignee Check Tests")
    class IsTaskOwnerOrAssigneeTests {

        @Test
        @DisplayName("Should return true when user is task creator")
        void shouldReturnTrueWhenUserIsCreator() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));

            // When
            boolean isOwnerOrAssignee = authorizationService.isTaskOwnerOrAssignee(taskId, userId);

            // Then
            assertThat(isOwnerOrAssignee).isTrue();
            verify(taskRepository).findById(taskId);
        }

        @Test
        @DisplayName("Should return true when user is assignee")
        void shouldReturnTrueWhenUserIsAssignee() {
            // Given
            UUID assigneeId = UUID.randomUUID();
            Task taskWithDifferentAssignee = Task.builder()
                    .id(taskId)
                    .title("Test Task")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .organizationId(organizationId)
                    .assigneeId(assigneeId)
                    .creatorId(UUID.randomUUID())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskWithDifferentAssignee));

            // When
            boolean isOwnerOrAssignee = authorizationService.isTaskOwnerOrAssignee(taskId, assigneeId);

            // Then
            assertThat(isOwnerOrAssignee).isTrue();
            verify(taskRepository).findById(taskId);
        }

        @Test
        @DisplayName("Should return false when user is neither creator nor assignee")
        void shouldReturnFalseWhenUserIsNeitherCreatorNorAssignee() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));

            // When
            boolean isOwnerOrAssignee = authorizationService.isTaskOwnerOrAssignee(taskId, otherUserId);

            // Then
            assertThat(isOwnerOrAssignee).isFalse();
            verify(taskRepository).findById(taskId);
        }

        @Test
        @DisplayName("Should return false when task not found")
        void shouldReturnFalseWhenTaskNotFound() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            // When
            boolean isOwnerOrAssignee = authorizationService.isTaskOwnerOrAssignee(taskId, userId);

            // Then
            assertThat(isOwnerOrAssignee).isFalse();
            verify(taskRepository).findById(taskId);
        }
    }

    @Nested
    @DisplayName("Check Organization Access Tests")
    class CheckOrganizationAccessTests {

        @Test
        @DisplayName("Should not throw exception when user is member")
        void shouldNotThrowExceptionWhenUserIsMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(regularMember));

            // When & Then
            authorizationService.checkOrganizationAccess(organizationId, userId);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not member")
        void shouldThrowExceptionWhenUserIsNotMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authorizationService.checkOrganizationAccess(organizationId, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }
    }

    @Nested
    @DisplayName("Check Organization Admin Access Tests")
    class CheckOrganizationAdminAccessTests {

        @Test
        @DisplayName("Should not throw exception when user is owner")
        void shouldNotThrowExceptionWhenUserIsOwner() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(ownerMember));

            // When & Then
            authorizationService.checkOrganizationAdminAccess(organizationId, userId);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should not throw exception when user is admin")
        void shouldNotThrowExceptionWhenUserIsAdmin() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(adminMember));

            // When & Then
            authorizationService.checkOrganizationAdminAccess(organizationId, userId);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is regular member")
        void shouldThrowExceptionWhenUserIsRegularMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(regularMember));

            // When & Then
            assertThatThrownBy(() -> authorizationService.checkOrganizationAdminAccess(organizationId, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is viewer")
        void shouldThrowExceptionWhenUserIsViewer() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.of(viewerMember));

            // When & Then
            assertThatThrownBy(() -> authorizationService.checkOrganizationAdminAccess(organizationId, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not member")
        void shouldThrowExceptionWhenUserIsNotMember() {
            // Given
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authorizationService.checkOrganizationAdminAccess(organizationId, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(memberRepository).findByOrganizationIdAndUserId(organizationId, userId);
        }
    }
}
