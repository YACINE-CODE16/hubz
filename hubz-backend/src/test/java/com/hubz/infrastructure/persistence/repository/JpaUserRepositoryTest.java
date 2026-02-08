package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("JpaUserRepository Tests")
class JpaUserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaUserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .email("test@example.com")
                .password("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .description("Test user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email when exists")
        void shouldFindUserByEmail() {
            // Given
            entityManager.persistAndFlush(testUser);

            // When
            Optional<UserEntity> found = userRepository.findByEmail("test@example.com");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return empty when email does not exist")
        void shouldReturnEmptyWhenEmailNotFound() {
            // Given
            entityManager.persistAndFlush(testUser);

            // When
            Optional<UserEntity> found = userRepository.findByEmail("nonexistent@example.com");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find user with case-sensitive email")
        void shouldFindUserCaseSensitive() {
            // Given
            entityManager.persistAndFlush(testUser);

            // When
            Optional<UserEntity> found = userRepository.findByEmail("Test@Example.com");

            // Then - depends on database collation, but typically case-sensitive
            // This test documents the expected behavior
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            // Given
            entityManager.persistAndFlush(testUser);

            // When
            boolean exists = userRepository.existsByEmail("test@example.com");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailNotFound() {
            // Given
            entityManager.persistAndFlush(testUser);

            // When
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find user by ID")
        void shouldSaveAndFindById() {
            // Given
            UserEntity saved = entityManager.persistAndFlush(testUser);

            // When
            Optional<UserEntity> found = userRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should update user")
        void shouldUpdateUser() {
            // Given
            UserEntity saved = entityManager.persistAndFlush(testUser);

            // When
            saved.setFirstName("Jane");
            saved.setLastName("Smith");
            saved.setUpdatedAt(LocalDateTime.now());
            userRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<UserEntity> updated = userRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getFirstName()).isEqualTo("Jane");
            assertThat(updated.get().getLastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() {
            // Given
            UserEntity saved = entityManager.persistAndFlush(testUser);
            UUID userId = saved.getId();

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            Optional<UserEntity> deleted = userRepository.findById(userId);
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should count all users")
        void shouldCountUsers() {
            // Given
            entityManager.persistAndFlush(testUser);

            UserEntity anotherUser = UserEntity.builder()
                    .email("another@example.com")
                    .password("password")
                    .firstName("Another")
                    .lastName("User")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherUser);

            // When
            long count = userRepository.count();

            // Then
            assertThat(count).isEqualTo(2);
        }
    }
}
