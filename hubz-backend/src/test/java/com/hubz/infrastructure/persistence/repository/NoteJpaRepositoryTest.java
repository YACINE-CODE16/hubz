package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("NoteJpaRepository Tests")
class NoteJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoteJpaRepository noteRepository;

    private UUID organizationId;
    private UUID createdById;
    private NoteEntity testNote;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        createdById = UUID.randomUUID();

        testNote = NoteEntity.builder()
                .title("Test Note")
                .content("This is test note content")
                .category("General")
                .organizationId(organizationId)
                .createdById(createdById)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationIdOrderByUpdatedAtDesc")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find notes by organization ordered by updated date")
        void shouldFindNotesByOrganizationOrderedByUpdatedAt() {
            // Given
            NoteEntity olderNote = NoteEntity.builder()
                    .title("Older Note")
                    .content("Content")
                    .organizationId(organizationId)
                    .createdById(createdById)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusDays(2))
                    .build();
            entityManager.persistAndFlush(olderNote);

            NoteEntity newerNote = NoteEntity.builder()
                    .title("Newer Note")
                    .content("Content")
                    .organizationId(organizationId)
                    .createdById(createdById)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(newerNote);

            // When
            List<NoteEntity> notes = noteRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);

            // Then
            assertThat(notes).hasSize(2);
            assertThat(notes.get(0).getTitle()).isEqualTo("Newer Note");
            assertThat(notes.get(1).getTitle()).isEqualTo("Older Note");
        }

        @Test
        @DisplayName("Should return empty list when no notes in organization")
        void shouldReturnEmptyListWhenNoNotes() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<NoteEntity> notes = noteRepository.findByOrganizationIdOrderByUpdatedAtDesc(emptyOrgId);

            // Then
            assertThat(notes).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOrganizationIdAndCategoryOrderByUpdatedAtDesc")
    class FindByOrganizationIdAndCategoryTests {

        @Test
        @DisplayName("Should find notes by organization and category")
        void shouldFindNotesByOrganizationAndCategory() {
            // Given
            entityManager.persistAndFlush(testNote);

            NoteEntity anotherNote = NoteEntity.builder()
                    .title("Meeting Note")
                    .content("Meeting content")
                    .category("Meetings")
                    .organizationId(organizationId)
                    .createdById(createdById)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherNote);

            // When
            List<NoteEntity> generalNotes = noteRepository
                    .findByOrganizationIdAndCategoryOrderByUpdatedAtDesc(organizationId, "General");

            // Then
            assertThat(generalNotes).hasSize(1);
            assertThat(generalNotes.get(0).getCategory()).isEqualTo("General");
        }

        @Test
        @DisplayName("Should return empty list when category has no notes")
        void shouldReturnEmptyListWhenCategoryEmpty() {
            // Given
            entityManager.persistAndFlush(testNote);

            // When
            List<NoteEntity> notes = noteRepository
                    .findByOrganizationIdAndCategoryOrderByUpdatedAtDesc(organizationId, "NonexistentCategory");

            // Then
            assertThat(notes).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchByTitleOrContent")
    class SearchByTitleOrContentTests {

        @Test
        @DisplayName("Should find notes matching title query")
        void shouldFindNotesByTitle() {
            // Given
            entityManager.persistAndFlush(testNote);

            // When
            List<NoteEntity> results = noteRepository.searchByTitleOrContent("Test", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Test Note");
        }

        @Test
        @DisplayName("Should find notes matching content query")
        void shouldFindNotesByContent() {
            // Given
            entityManager.persistAndFlush(testNote);

            // When
            List<NoteEntity> results = noteRepository.searchByTitleOrContent("test note content", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should search case-insensitively")
        void shouldSearchCaseInsensitive() {
            // Given
            entityManager.persistAndFlush(testNote);

            // When
            List<NoteEntity> results = noteRepository.searchByTitleOrContent("TEST", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should only search within specified organizations")
        void shouldLimitSearchToSpecifiedOrganizations() {
            // Given
            entityManager.persistAndFlush(testNote);

            UUID anotherOrgId = UUID.randomUUID();
            NoteEntity anotherNote = NoteEntity.builder()
                    .title("Test Note in Another Org")
                    .content("Content")
                    .organizationId(anotherOrgId)
                    .createdById(createdById)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherNote);

            // When - Only search in first organization
            List<NoteEntity> results = noteRepository.searchByTitleOrContent("Test", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getOrganizationId()).isEqualTo(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            entityManager.persistAndFlush(testNote);

            // When
            List<NoteEntity> results = noteRepository.searchByTitleOrContent("Nonexistent", List.of(organizationId));

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find note by ID")
        void shouldSaveAndFindById() {
            // Given
            NoteEntity saved = entityManager.persistAndFlush(testNote);

            // When
            var found = noteRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Note");
            assertThat(found.get().getContent()).isEqualTo("This is test note content");
            assertThat(found.get().getCategory()).isEqualTo("General");
        }

        @Test
        @DisplayName("Should update note")
        void shouldUpdateNote() {
            // Given
            NoteEntity saved = entityManager.persistAndFlush(testNote);

            // When
            saved.setTitle("Updated Note");
            saved.setContent("Updated content");
            saved.setUpdatedAt(LocalDateTime.now());
            noteRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            var updated = noteRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getTitle()).isEqualTo("Updated Note");
            assertThat(updated.get().getContent()).isEqualTo("Updated content");
        }

        @Test
        @DisplayName("Should delete note")
        void shouldDeleteNote() {
            // Given
            NoteEntity saved = entityManager.persistAndFlush(testNote);
            UUID noteId = saved.getId();

            // When
            noteRepository.deleteById(noteId);
            entityManager.flush();

            // Then
            var deleted = noteRepository.findById(noteId);
            assertThat(deleted).isEmpty();
        }
    }
}
