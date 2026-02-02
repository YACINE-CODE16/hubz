package com.hubz.application.service;

import com.hubz.application.dto.response.SearchResultResponse;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Unit Tests")
class SearchServiceTest {

    @Mock
    private OrganizationRepositoryPort organizationRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private SearchService searchService;

    private UUID userId;
    private UUID orgId;
    private Organization testOrg;
    private OrganizationMember testMember;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        testOrg = Organization.builder()
                .id(orgId)
                .name("Test Organization")
                .description("Test description")
                .icon("icon.png")
                .color("#3B82F6")
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        testMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .userId(userId)
                .role(MemberRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Search with empty or null query")
    class EmptyQueryTests {

        @Test
        @DisplayName("Should return empty results for null query")
        void shouldReturnEmptyResultsForNullQuery() {
            // When
            SearchResultResponse results = searchService.search(null, userId);

            // Then
            assertThat(results.getTotalResults()).isZero();
            assertThat(results.getOrganizations()).isEmpty();
            assertThat(results.getTasks()).isEmpty();
            assertThat(results.getGoals()).isEmpty();
            assertThat(results.getEvents()).isEmpty();
            assertThat(results.getNotes()).isEmpty();
            assertThat(results.getMembers()).isEmpty();

            verify(organizationRepository, never()).searchByName(anyString());
        }

        @Test
        @DisplayName("Should return empty results for empty query")
        void shouldReturnEmptyResultsForEmptyQuery() {
            // When
            SearchResultResponse results = searchService.search("", userId);

            // Then
            assertThat(results.getTotalResults()).isZero();
            verify(organizationRepository, never()).searchByName(anyString());
        }

        @Test
        @DisplayName("Should return empty results for whitespace-only query")
        void shouldReturnEmptyResultsForWhitespaceQuery() {
            // When
            SearchResultResponse results = searchService.search("   ", userId);

            // Then
            assertThat(results.getTotalResults()).isZero();
            verify(organizationRepository, never()).searchByName(anyString());
        }
    }

    @Nested
    @DisplayName("Search organizations")
    class OrganizationSearchTests {

        @Test
        @DisplayName("Should find organizations by name")
        void shouldFindOrganizationsByName() {
            // Given
            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("test")).thenReturn(List.of(testOrg));
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("test", userId);

            // Then
            assertThat(results.getOrganizations()).hasSize(1);
            assertThat(results.getOrganizations().get(0).getName()).isEqualTo("Test Organization");
            assertThat(results.getOrganizations().get(0).getMatchedField()).isEqualTo("name");
        }

        @Test
        @DisplayName("Should only return organizations user has access to")
        void shouldOnlyReturnAccessibleOrganizations() {
            // Given
            Organization otherOrg = Organization.builder()
                    .id(UUID.randomUUID())
                    .name("Test Other Org")
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("test")).thenReturn(List.of(testOrg, otherOrg));
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("test", userId);

            // Then
            assertThat(results.getOrganizations()).hasSize(1);
            assertThat(results.getOrganizations().get(0).getId()).isEqualTo(orgId.toString());
        }
    }

    @Nested
    @DisplayName("Search tasks")
    class TaskSearchTests {

        @Test
        @DisplayName("Should find tasks by title")
        void shouldFindTasksByTitle() {
            // Given
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Important Task")
                    .description("Task description")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH)
                    .organizationId(orgId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("important")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription("important", List.of(orgId))).thenReturn(List.of(task));
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("important", userId);

            // Then
            assertThat(results.getTasks()).hasSize(1);
            assertThat(results.getTasks().get(0).getTitle()).isEqualTo("Important Task");
            assertThat(results.getTasks().get(0).getStatus()).isEqualTo("TODO");
            assertThat(results.getTasks().get(0).getOrganizationName()).isEqualTo("Test Organization");
        }
    }

    @Nested
    @DisplayName("Search goals")
    class GoalSearchTests {

        @Test
        @DisplayName("Should find goals by title")
        void shouldFindGoalsByTitle() {
            // Given
            Goal goal = Goal.builder()
                    .id(UUID.randomUUID())
                    .title("Sales Goal")
                    .description("Increase sales by 20%")
                    .type(GoalType.MEDIUM)
                    .deadline(LocalDate.now().plusMonths(3))
                    .organizationId(orgId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("sales")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle("sales", List.of(orgId), userId)).thenReturn(List.of(goal));
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("sales", userId);

            // Then
            assertThat(results.getGoals()).hasSize(1);
            assertThat(results.getGoals().get(0).getTitle()).isEqualTo("Sales Goal");
            assertThat(results.getGoals().get(0).getType()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Should handle personal goals without organization")
        void shouldHandlePersonalGoals() {
            // Given
            Goal personalGoal = Goal.builder()
                    .id(UUID.randomUUID())
                    .title("Personal Fitness Goal")
                    .type(GoalType.SHORT)
                    .organizationId(null)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("fitness")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle("fitness", List.of(orgId), userId)).thenReturn(List.of(personalGoal));
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("fitness", userId);

            // Then
            assertThat(results.getGoals()).hasSize(1);
            assertThat(results.getGoals().get(0).getOrganizationId()).isNull();
            assertThat(results.getGoals().get(0).getOrganizationName()).isEqualTo("Personal");
        }
    }

    @Nested
    @DisplayName("Search events")
    class EventSearchTests {

        @Test
        @DisplayName("Should find events by title or description")
        void shouldFindEventsByTitleOrDescription() {
            // Given
            Event event = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Team Meeting")
                    .description("Weekly sync")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .organizationId(orgId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("meeting")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription("meeting", List.of(orgId), userId)).thenReturn(List.of(event));
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("meeting", userId);

            // Then
            assertThat(results.getEvents()).hasSize(1);
            assertThat(results.getEvents().get(0).getTitle()).isEqualTo("Team Meeting");
        }
    }

    @Nested
    @DisplayName("Search notes")
    class NoteSearchTests {

        @Test
        @DisplayName("Should find notes by title or content")
        void shouldFindNotesByTitleOrContent() {
            // Given
            Note note = Note.builder()
                    .id(UUID.randomUUID())
                    .title("Project Documentation")
                    .content("This is the documentation for our project...")
                    .category("documentation")
                    .organizationId(orgId)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("documentation")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent("documentation", List.of(orgId))).thenReturn(List.of(note));

            // When
            SearchResultResponse results = searchService.search("documentation", userId);

            // Then
            assertThat(results.getNotes()).hasSize(1);
            assertThat(results.getNotes().get(0).getTitle()).isEqualTo("Project Documentation");
            assertThat(results.getNotes().get(0).getCategory()).isEqualTo("documentation");
        }
    }

    @Nested
    @DisplayName("Search members")
    class MemberSearchTests {

        @Test
        @DisplayName("Should find members by name")
        void shouldFindMembersByName() {
            // Given
            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("john")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());
            when(memberRepository.findByOrganizationId(orgId)).thenReturn(List.of(testMember));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            SearchResultResponse results = searchService.search("john", userId);

            // Then
            assertThat(results.getMembers()).hasSize(1);
            assertThat(results.getMembers().get(0).getFirstName()).isEqualTo("John");
            assertThat(results.getMembers().get(0).getLastName()).isEqualTo("Doe");
            assertThat(results.getMembers().get(0).getMatchedField()).isEqualTo("name");
        }

        @Test
        @DisplayName("Should find members by email")
        void shouldFindMembersByEmail() {
            // Given
            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("test@example")).thenReturn(List.of());
            when(taskRepository.searchByTitleOrDescription(anyString(), anyList())).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());
            when(memberRepository.findByOrganizationId(orgId)).thenReturn(List.of(testMember));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            SearchResultResponse results = searchService.search("test@example", userId);

            // Then
            assertThat(results.getMembers()).hasSize(1);
            assertThat(results.getMembers().get(0).getEmail()).isEqualTo("test@example.com");
            assertThat(results.getMembers().get(0).getMatchedField()).isEqualTo("email");
        }
    }

    @Nested
    @DisplayName("Total results calculation")
    class TotalResultsTests {

        @Test
        @DisplayName("Should calculate correct total results")
        void shouldCalculateCorrectTotalResults() {
            // Given
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Test Task")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .organizationId(orgId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(memberRepository.findByUserId(userId)).thenReturn(List.of(testMember));
            when(organizationRepository.findAll()).thenReturn(List.of(testOrg));
            when(organizationRepository.searchByName("test")).thenReturn(List.of(testOrg));
            when(taskRepository.searchByTitleOrDescription("test", List.of(orgId))).thenReturn(List.of(task));
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());
            when(noteRepository.searchByTitleOrContent(anyString(), anyList())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("test", userId);

            // Then
            assertThat(results.getTotalResults()).isEqualTo(2); // 1 org + 1 task
        }
    }

    @Nested
    @DisplayName("User without organizations")
    class NoOrganizationsTests {

        @Test
        @DisplayName("Should handle user with no organizations")
        void shouldHandleUserWithNoOrganizations() {
            // Given
            when(memberRepository.findByUserId(userId)).thenReturn(List.of());
            when(organizationRepository.findAll()).thenReturn(List.of());
            when(organizationRepository.searchByName("test")).thenReturn(List.of());
            when(goalRepository.searchByTitle(anyString(), anyList(), any())).thenReturn(List.of());
            when(eventRepository.searchByTitleOrDescription(anyString(), anyList(), any())).thenReturn(List.of());

            // When
            SearchResultResponse results = searchService.search("test", userId);

            // Then
            assertThat(results.getTotalResults()).isZero();
            assertThat(results.getTasks()).isEmpty();
            assertThat(results.getNotes()).isEmpty();
        }
    }
}
