package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TeamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("TeamJpaRepository Tests")
class TeamJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeamJpaRepository teamRepository;

    private UUID organizationId;
    private TeamEntity testTeam;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();

        testTeam = TeamEntity.builder()
                .name("Engineering Team")
                .description("The engineering team")
                .organizationId(organizationId)
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationIdOrderByCreatedAtDesc")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find teams ordered by creation date descending")
        void shouldFindTeamsOrderedByCreatedAtDesc() throws InterruptedException {
            // Given - Create teams with slight delay to ensure different timestamps
            TeamEntity olderTeam = TeamEntity.builder()
                    .name("Older Team")
                    .organizationId(organizationId)
                    .build();
            entityManager.persistAndFlush(olderTeam);

            // Small delay to ensure different timestamps
            Thread.sleep(10);

            TeamEntity newerTeam = TeamEntity.builder()
                    .name("Newer Team")
                    .organizationId(organizationId)
                    .build();
            entityManager.persistAndFlush(newerTeam);

            // When
            List<TeamEntity> teams = teamRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);

            // Then
            assertThat(teams).hasSize(2);
            assertThat(teams.get(0).getName()).isEqualTo("Newer Team");
            assertThat(teams.get(1).getName()).isEqualTo("Older Team");
        }

        @Test
        @DisplayName("Should return empty list when organization has no teams")
        void shouldReturnEmptyListWhenNoTeams() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<TeamEntity> teams = teamRepository.findByOrganizationIdOrderByCreatedAtDesc(emptyOrgId);

            // Then
            assertThat(teams).isEmpty();
        }

        @Test
        @DisplayName("Should only return teams for the specified organization")
        void shouldOnlyReturnTeamsForSpecifiedOrganization() {
            // Given
            entityManager.persistAndFlush(testTeam);

            UUID anotherOrgId = UUID.randomUUID();
            TeamEntity anotherOrgTeam = TeamEntity.builder()
                    .name("Another Org Team")
                    .organizationId(anotherOrgId)
                    .build();
            entityManager.persistAndFlush(anotherOrgTeam);

            // When
            List<TeamEntity> teams = teamRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);

            // Then
            assertThat(teams).hasSize(1);
            assertThat(teams.get(0).getName()).isEqualTo("Engineering Team");
            assertThat(teams.get(0).getOrganizationId()).isEqualTo(organizationId);
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find team by ID")
        void shouldSaveAndFindById() {
            // Given
            TeamEntity saved = entityManager.persistAndFlush(testTeam);

            // When
            Optional<TeamEntity> found = teamRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Engineering Team");
            assertThat(found.get().getDescription()).isEqualTo("The engineering team");
            assertThat(found.get().getOrganizationId()).isEqualTo(organizationId);
        }

        @Test
        @DisplayName("Should update team")
        void shouldUpdateTeam() {
            // Given
            TeamEntity saved = entityManager.persistAndFlush(testTeam);

            // When
            saved.setName("Updated Team Name");
            saved.setDescription("Updated description");
            teamRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<TeamEntity> updated = teamRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("Updated Team Name");
            assertThat(updated.get().getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should delete team")
        void shouldDeleteTeam() {
            // Given
            TeamEntity saved = entityManager.persistAndFlush(testTeam);
            UUID teamId = saved.getId();

            // When
            teamRepository.deleteById(teamId);
            entityManager.flush();

            // Then
            Optional<TeamEntity> deleted = teamRepository.findById(teamId);
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should count teams in organization")
        void shouldCountTeamsInOrganization() {
            // Given
            entityManager.persistAndFlush(testTeam);

            TeamEntity anotherTeam = TeamEntity.builder()
                    .name("Design Team")
                    .organizationId(organizationId)
                    .build();
            entityManager.persistAndFlush(anotherTeam);

            // When
            List<TeamEntity> teams = teamRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);

            // Then
            assertThat(teams).hasSize(2);
        }

        @Test
        @DisplayName("Should find all teams")
        void shouldFindAllTeams() {
            // Given
            entityManager.persistAndFlush(testTeam);

            UUID anotherOrgId = UUID.randomUUID();
            TeamEntity anotherOrgTeam = TeamEntity.builder()
                    .name("Another Org Team")
                    .organizationId(anotherOrgId)
                    .build();
            entityManager.persistAndFlush(anotherOrgTeam);

            // When
            List<TeamEntity> allTeams = teamRepository.findAll();

            // Then
            assertThat(allTeams).hasSize(2);
        }
    }
}
