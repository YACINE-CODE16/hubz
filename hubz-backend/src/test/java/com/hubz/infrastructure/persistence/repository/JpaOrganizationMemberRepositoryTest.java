package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.MemberRole;
import com.hubz.infrastructure.persistence.entity.OrganizationMemberEntity;
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
@DisplayName("JpaOrganizationMemberRepository Tests")
class JpaOrganizationMemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaOrganizationMemberRepository memberRepository;

    private UUID organizationId;
    private UUID userId;
    private OrganizationMemberEntity testMember;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testMember = OrganizationMemberEntity.builder()
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationId")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find all members of an organization")
        void shouldFindMembersByOrganizationId() {
            // Given
            entityManager.persistAndFlush(testMember);

            UUID anotherUserId = UUID.randomUUID();
            OrganizationMemberEntity anotherMember = OrganizationMemberEntity.builder()
                    .organizationId(organizationId)
                    .userId(anotherUserId)
                    .role(MemberRole.ADMIN)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherMember);

            // When
            List<OrganizationMemberEntity> members = memberRepository.findByOrganizationId(organizationId);

            // Then
            assertThat(members).hasSize(2);
            assertThat(members).extracting(OrganizationMemberEntity::getOrganizationId)
                    .containsOnly(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no members")
        void shouldReturnEmptyListWhenNoMembers() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<OrganizationMemberEntity> members = memberRepository.findByOrganizationId(emptyOrgId);

            // Then
            assertThat(members).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOrganizationIdAndUserId")
    class FindByOrganizationIdAndUserIdTests {

        @Test
        @DisplayName("Should find member by organization and user")
        void shouldFindMember() {
            // Given
            entityManager.persistAndFlush(testMember);

            // When
            Optional<OrganizationMemberEntity> found = memberRepository
                    .findByOrganizationIdAndUserId(organizationId, userId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo(MemberRole.MEMBER);
        }

        @Test
        @DisplayName("Should return empty when member does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            entityManager.persistAndFlush(testMember);

            // When
            Optional<OrganizationMemberEntity> found = memberRepository
                    .findByOrganizationIdAndUserId(organizationId, UUID.randomUUID());

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByOrganizationIdAndUserId")
    class ExistsByOrganizationIdAndUserIdTests {

        @Test
        @DisplayName("Should return true when membership exists")
        void shouldReturnTrueWhenExists() {
            // Given
            entityManager.persistAndFlush(testMember);

            // When
            boolean exists = memberRepository.existsByOrganizationIdAndUserId(organizationId, userId);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when membership does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Given
            entityManager.persistAndFlush(testMember);

            // When
            boolean exists = memberRepository.existsByOrganizationIdAndUserId(organizationId, UUID.randomUUID());

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find all memberships for a user")
        void shouldFindMembershipsByUserId() {
            // Given
            entityManager.persistAndFlush(testMember);

            UUID anotherOrgId = UUID.randomUUID();
            OrganizationMemberEntity anotherMembership = OrganizationMemberEntity.builder()
                    .organizationId(anotherOrgId)
                    .userId(userId)
                    .role(MemberRole.OWNER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherMembership);

            // When
            List<OrganizationMemberEntity> memberships = memberRepository.findByUserId(userId);

            // Then
            assertThat(memberships).hasSize(2);
            assertThat(memberships).extracting(OrganizationMemberEntity::getUserId)
                    .containsOnly(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no memberships")
        void shouldReturnEmptyListWhenNoMemberships() {
            // Given
            UUID userWithNoMemberships = UUID.randomUUID();

            // When
            List<OrganizationMemberEntity> memberships = memberRepository.findByUserId(userWithNoMemberships);

            // Then
            assertThat(memberships).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByOrganizationIdAndUserId")
    class DeleteByOrganizationIdAndUserIdTests {

        @Test
        @DisplayName("Should delete membership")
        void shouldDeleteMembership() {
            // Given
            entityManager.persistAndFlush(testMember);

            // When
            memberRepository.deleteByOrganizationIdAndUserId(organizationId, userId);
            entityManager.flush();

            // Then
            boolean exists = memberRepository.existsByOrganizationIdAndUserId(organizationId, userId);
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Role Tests")
    class RoleTests {

        @Test
        @DisplayName("Should save member with different roles")
        void shouldSaveMemberWithDifferentRoles() {
            // Given & When - Save members with different roles
            OrganizationMemberEntity owner = OrganizationMemberEntity.builder()
                    .organizationId(organizationId)
                    .userId(UUID.randomUUID())
                    .role(MemberRole.OWNER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(owner);

            OrganizationMemberEntity admin = OrganizationMemberEntity.builder()
                    .organizationId(organizationId)
                    .userId(UUID.randomUUID())
                    .role(MemberRole.ADMIN)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(admin);

            OrganizationMemberEntity member = OrganizationMemberEntity.builder()
                    .organizationId(organizationId)
                    .userId(UUID.randomUUID())
                    .role(MemberRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(member);

            OrganizationMemberEntity viewer = OrganizationMemberEntity.builder()
                    .organizationId(organizationId)
                    .userId(UUID.randomUUID())
                    .role(MemberRole.VIEWER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(viewer);

            // Then
            List<OrganizationMemberEntity> members = memberRepository.findByOrganizationId(organizationId);
            assertThat(members).hasSize(4);
            assertThat(members).extracting(OrganizationMemberEntity::getRole)
                    .containsExactlyInAnyOrder(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.VIEWER);
        }

        @Test
        @DisplayName("Should update member role")
        void shouldUpdateMemberRole() {
            // Given
            OrganizationMemberEntity saved = entityManager.persistAndFlush(testMember);

            // When
            saved.setRole(MemberRole.ADMIN);
            memberRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<OrganizationMemberEntity> updated = memberRepository
                    .findByOrganizationIdAndUserId(organizationId, userId);
            assertThat(updated).isPresent();
            assertThat(updated.get().getRole()).isEqualTo(MemberRole.ADMIN);
        }
    }
}
