package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.OrganizationEntity;
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
@DisplayName("JpaOrganizationRepository Tests")
class JpaOrganizationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaOrganizationRepository organizationRepository;

    private UUID ownerId;
    private OrganizationEntity testOrganization;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        testOrganization = OrganizationEntity.builder()
                .name("Test Organization")
                .description("A test organization")
                .icon("building")
                .color("#3B82F6")
                .readme("# Welcome")
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("searchByName")
    class SearchByNameTests {

        @Test
        @DisplayName("Should find organizations matching name query")
        void shouldFindOrganizationsByName() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            OrganizationEntity another = OrganizationEntity.builder()
                    .name("Acme Corporation")
                    .description("Another org")
                    .ownerId(ownerId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(another);

            // When
            List<OrganizationEntity> results = organizationRepository.searchByName("Test");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Test Organization");
        }

        @Test
        @DisplayName("Should find organizations with case-insensitive search")
        void shouldSearchCaseInsensitive() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            // When
            List<OrganizationEntity> results = organizationRepository.searchByName("test");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Test Organization");
        }

        @Test
        @DisplayName("Should find organizations with partial match")
        void shouldFindWithPartialMatch() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            // When
            List<OrganizationEntity> results = organizationRepository.searchByName("Organ");

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            // When
            List<OrganizationEntity> results = organizationRepository.searchByName("Nonexistent");

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple matching organizations")
        void shouldReturnMultipleMatches() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            OrganizationEntity another = OrganizationEntity.builder()
                    .name("Test Company")
                    .description("Another test org")
                    .ownerId(ownerId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(another);

            // When
            List<OrganizationEntity> results = organizationRepository.searchByName("Test");

            // Then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find organization by ID")
        void shouldSaveAndFindById() {
            // Given
            OrganizationEntity saved = entityManager.persistAndFlush(testOrganization);

            // When
            Optional<OrganizationEntity> found = organizationRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Test Organization");
            assertThat(found.get().getDescription()).isEqualTo("A test organization");
            assertThat(found.get().getOwnerId()).isEqualTo(ownerId);
        }

        @Test
        @DisplayName("Should update organization")
        void shouldUpdateOrganization() {
            // Given
            OrganizationEntity saved = entityManager.persistAndFlush(testOrganization);

            // When
            saved.setName("Updated Organization");
            saved.setDescription("Updated description");
            organizationRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<OrganizationEntity> updated = organizationRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("Updated Organization");
            assertThat(updated.get().getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should delete organization")
        void shouldDeleteOrganization() {
            // Given
            OrganizationEntity saved = entityManager.persistAndFlush(testOrganization);
            UUID orgId = saved.getId();

            // When
            organizationRepository.deleteById(orgId);
            entityManager.flush();

            // Then
            Optional<OrganizationEntity> deleted = organizationRepository.findById(orgId);
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should find all organizations")
        void shouldFindAllOrganizations() {
            // Given
            entityManager.persistAndFlush(testOrganization);

            OrganizationEntity another = OrganizationEntity.builder()
                    .name("Another Org")
                    .ownerId(ownerId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(another);

            // When
            List<OrganizationEntity> all = organizationRepository.findAll();

            // Then
            assertThat(all).hasSize(2);
        }
    }
}
