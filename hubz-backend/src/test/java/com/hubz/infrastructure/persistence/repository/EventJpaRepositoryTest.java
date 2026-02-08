package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.EventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("EventJpaRepository Tests")
class EventJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventJpaRepository eventRepository;

    private UUID organizationId;
    private UUID userId;
    private EventEntity testEvent;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testEvent = EventEntity.builder()
                .title("Test Event")
                .description("A test event description")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .objective("Test objective")
                .organizationId(organizationId)
                .userId(userId)
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationIdOrderByStartTimeAsc")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find events ordered by start time")
        void shouldFindEventsOrderedByStartTime() {
            // Given
            EventEntity laterEvent = EventEntity.builder()
                    .title("Later Event")
                    .startTime(LocalDateTime.now().plusDays(3))
                    .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
                    .organizationId(organizationId)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(laterEvent);

            EventEntity earlierEvent = EventEntity.builder()
                    .title("Earlier Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .organizationId(organizationId)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(earlierEvent);

            // When
            List<EventEntity> events = eventRepository.findByOrganizationIdOrderByStartTimeAsc(organizationId);

            // Then
            assertThat(events).hasSize(2);
            assertThat(events.get(0).getTitle()).isEqualTo("Earlier Event");
            assertThat(events.get(1).getTitle()).isEqualTo("Later Event");
        }

        @Test
        @DisplayName("Should return empty list when no events in organization")
        void shouldReturnEmptyListWhenNoEvents() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<EventEntity> events = eventRepository.findByOrganizationIdOrderByStartTimeAsc(emptyOrgId);

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPersonalEvents")
    class FindPersonalEventsTests {

        @Test
        @DisplayName("Should find personal events (organization is null)")
        void shouldFindPersonalEvents() {
            // Given
            EventEntity personalEvent = EventEntity.builder()
                    .title("Personal Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(personalEvent);

            // Create organization event
            entityManager.persistAndFlush(testEvent);

            // When
            List<EventEntity> personalEvents = eventRepository.findPersonalEvents(userId);

            // Then
            assertThat(personalEvents).hasSize(1);
            assertThat(personalEvents.get(0).getTitle()).isEqualTo("Personal Event");
            assertThat(personalEvents.get(0).getOrganizationId()).isNull();
        }

        @Test
        @DisplayName("Should return personal events ordered by start time")
        void shouldReturnPersonalEventsOrderedByStartTime() {
            // Given
            EventEntity laterEvent = EventEntity.builder()
                    .title("Later Personal Event")
                    .startTime(LocalDateTime.now().plusDays(3))
                    .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(laterEvent);

            EventEntity earlierEvent = EventEntity.builder()
                    .title("Earlier Personal Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(earlierEvent);

            // When
            List<EventEntity> events = eventRepository.findPersonalEvents(userId);

            // Then
            assertThat(events).hasSize(2);
            assertThat(events.get(0).getTitle()).isEqualTo("Earlier Personal Event");
            assertThat(events.get(1).getTitle()).isEqualTo("Later Personal Event");
        }
    }

    @Nested
    @DisplayName("findByOrganizationAndTimeRange")
    class FindByOrganizationAndTimeRangeTests {

        @Test
        @DisplayName("Should find events within time range")
        void shouldFindEventsInTimeRange() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = now.plusDays(1);
            LocalDateTime end = now.plusDays(7);

            EventEntity inRangeEvent = EventEntity.builder()
                    .title("In Range Event")
                    .startTime(now.plusDays(3))
                    .endTime(now.plusDays(3).plusHours(1))
                    .organizationId(organizationId)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(inRangeEvent);

            EventEntity outOfRangeEvent = EventEntity.builder()
                    .title("Out of Range Event")
                    .startTime(now.plusDays(10))
                    .endTime(now.plusDays(10).plusHours(1))
                    .organizationId(organizationId)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(outOfRangeEvent);

            // When
            List<EventEntity> events = eventRepository.findByOrganizationAndTimeRange(organizationId, start, end);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo("In Range Event");
        }
    }

    @Nested
    @DisplayName("findPersonalEventsByTimeRange")
    class FindPersonalEventsByTimeRangeTests {

        @Test
        @DisplayName("Should find personal events within time range")
        void shouldFindPersonalEventsInTimeRange() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = now.plusDays(1);
            LocalDateTime end = now.plusDays(7);

            EventEntity inRangeEvent = EventEntity.builder()
                    .title("In Range Personal Event")
                    .startTime(now.plusDays(3))
                    .endTime(now.plusDays(3).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(inRangeEvent);

            EventEntity outOfRangeEvent = EventEntity.builder()
                    .title("Out of Range Personal Event")
                    .startTime(now.plusDays(10))
                    .endTime(now.plusDays(10).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(outOfRangeEvent);

            // When
            List<EventEntity> events = eventRepository.findPersonalEventsByTimeRange(userId, start, end);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo("In Range Personal Event");
        }
    }

    @Nested
    @DisplayName("searchByTitleOrDescription")
    class SearchByTitleOrDescriptionTests {

        @Test
        @DisplayName("Should find events matching title query")
        void shouldFindEventsByTitle() {
            // Given
            entityManager.persistAndFlush(testEvent);

            // When
            List<EventEntity> results = eventRepository.searchByTitleOrDescription(
                    "Test", List.of(organizationId), userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Test Event");
        }

        @Test
        @DisplayName("Should find events matching description query")
        void shouldFindEventsByDescription() {
            // Given
            entityManager.persistAndFlush(testEvent);

            // When
            List<EventEntity> results = eventRepository.searchByTitleOrDescription(
                    "test event description", List.of(organizationId), userId);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should find personal events in search")
        void shouldFindPersonalEventsInSearch() {
            // Given
            EventEntity personalEvent = EventEntity.builder()
                    .title("Personal Test Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .organizationId(null)
                    .userId(userId)
                    .build();
            entityManager.persistAndFlush(personalEvent);

            // When
            List<EventEntity> results = eventRepository.searchByTitleOrDescription(
                    "Test", List.of(), userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Personal Test Event");
        }

        @Test
        @DisplayName("Should search case-insensitively")
        void shouldSearchCaseInsensitive() {
            // Given
            entityManager.persistAndFlush(testEvent);

            // When
            List<EventEntity> results = eventRepository.searchByTitleOrDescription(
                    "test", List.of(organizationId), userId);

            // Then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find event by ID")
        void shouldSaveAndFindById() {
            // Given
            EventEntity saved = entityManager.persistAndFlush(testEvent);

            // When
            Optional<EventEntity> found = eventRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Event");
            assertThat(found.get().getDescription()).isEqualTo("A test event description");
        }

        @Test
        @DisplayName("Should update event")
        void shouldUpdateEvent() {
            // Given
            EventEntity saved = entityManager.persistAndFlush(testEvent);

            // When
            saved.setTitle("Updated Event");
            saved.setDescription("Updated description");
            eventRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<EventEntity> updated = eventRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getTitle()).isEqualTo("Updated Event");
        }

        @Test
        @DisplayName("Should delete event")
        void shouldDeleteEvent() {
            // Given
            EventEntity saved = entityManager.persistAndFlush(testEvent);
            UUID eventId = saved.getId();

            // When
            eventRepository.deleteById(eventId);
            entityManager.flush();

            // Then
            Optional<EventEntity> deleted = eventRepository.findById(eventId);
            assertThat(deleted).isEmpty();
        }
    }
}
