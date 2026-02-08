package com.hubz.application.service;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentionService Unit Tests")
class MentionServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @InjectMocks
    private MentionService mentionService;

    private UUID organizationId;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private User user1;
    private User user2;
    private User user3;
    private OrganizationMember member1;
    private OrganizationMember member2;
    private OrganizationMember member3;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();

        user1 = User.builder()
                .id(user1Id)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .profilePhotoUrl("https://example.com/john.jpg")
                .build();

        user2 = User.builder()
                .id(user2Id)
                .email("jane.smith@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        user3 = User.builder()
                .id(user3Id)
                .email("bob.wilson@example.com")
                .firstName("Bob")
                .lastName("Wilson")
                .build();

        member1 = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(user1Id)
                .build();

        member2 = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(user2Id)
                .build();

        member3 = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(user3Id)
                .build();
    }

    @Nested
    @DisplayName("Extract Mentions")
    class ExtractMentionsTests {

        @Test
        @DisplayName("Should extract single mention from content")
        void shouldExtractSingleMention() {
            // Given
            String content = "Hello @john, how are you?";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactly("john");
        }

        @Test
        @DisplayName("Should extract multiple mentions from content")
        void shouldExtractMultipleMentions() {
            // Given
            String content = "Hey @john and @jane, please review this task.";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactlyInAnyOrder("john", "jane");
        }

        @Test
        @DisplayName("Should extract mention with dot notation (firstName.lastName)")
        void shouldExtractMentionWithDotNotation() {
            // Given
            String content = "Thanks @john.doe for your help!";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactly("john.doe");
        }

        @Test
        @DisplayName("Should extract mixed mentions")
        void shouldExtractMixedMentions() {
            // Given
            String content = "@john and @jane.smith should look at this.";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactlyInAnyOrder("john", "jane.smith");
        }

        @Test
        @DisplayName("Should return empty set for null content")
        void shouldReturnEmptySetForNullContent() {
            // Given
            String content = null;

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for blank content")
        void shouldReturnEmptySetForBlankContent() {
            // Given
            String content = "   ";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set when no mentions")
        void shouldReturnEmptySetWhenNoMentions() {
            // Given
            String content = "Hello everyone, please review this task.";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).isEmpty();
        }

        @Test
        @DisplayName("Should handle duplicate mentions")
        void shouldHandleDuplicateMentions() {
            // Given
            String content = "@john please help @john with this.";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactly("john");
        }

        @Test
        @DisplayName("Should handle mentions at start and end of content")
        void shouldHandleMentionsAtStartAndEnd() {
            // Given
            String content = "@john this is for @jane";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactlyInAnyOrder("john", "jane");
        }

        @Test
        @DisplayName("Should convert mentions to lowercase")
        void shouldConvertMentionsToLowercase() {
            // Given
            String content = "Hey @John and @JANE";

            // When
            Set<String> mentions = mentionService.extractMentions(content);

            // Then
            assertThat(mentions).containsExactlyInAnyOrder("john", "jane");
        }
    }

    @Nested
    @DisplayName("Resolve Mentions to User IDs")
    class ResolveMentionsTests {

        @Test
        @DisplayName("Should resolve mention by first name")
        void shouldResolveMentionByFirstName() {
            // Given
            Set<String> mentions = Set.of("john");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).containsExactly(user1Id);
        }

        @Test
        @DisplayName("Should resolve mention by last name")
        void shouldResolveMentionByLastName() {
            // Given
            Set<String> mentions = Set.of("smith");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).containsExactly(user2Id);
        }

        @Test
        @DisplayName("Should resolve mention by full name (firstName.lastName)")
        void shouldResolveMentionByFullName() {
            // Given
            Set<String> mentions = Set.of("john.doe");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).containsExactly(user1Id);
        }

        @Test
        @DisplayName("Should resolve multiple mentions")
        void shouldResolveMultipleMentions() {
            // Given
            Set<String> mentions = Set.of("john", "jane");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).containsExactlyInAnyOrder(user1Id, user2Id);
        }

        @Test
        @DisplayName("Should exclude specified user from resolved mentions")
        void shouldExcludeSpecifiedUser() {
            // Given
            Set<String> mentions = Set.of("john", "jane");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, user1Id);

            // Then
            assertThat(userIds).containsExactly(user2Id);
        }

        @Test
        @DisplayName("Should return empty set for unresolved mentions")
        void shouldReturnEmptySetForUnresolvedMentions() {
            // Given
            Set<String> mentions = Set.of("nonexistent");
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for empty mentions")
        void shouldReturnEmptySetForEmptyMentions() {
            // Given
            Set<String> mentions = Collections.emptySet();

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).isEmpty();
        }

        @Test
        @DisplayName("Should only resolve members of the organization")
        void shouldOnlyResolveMembersOfOrganization() {
            // Given
            Set<String> mentions = Set.of("john", "bob");
            // member3 (bob) is not in this organization
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));

            // When
            Set<UUID> userIds = mentionService.resolveMentionsToUserIds(mentions, organizationId, null);

            // Then
            assertThat(userIds).containsExactly(user1Id);
        }
    }

    @Nested
    @DisplayName("Parse Mentions and Resolve")
    class ParseMentionsAndResolveTests {

        @Test
        @DisplayName("Should parse content and resolve mentions")
        void shouldParseContentAndResolveMentions() {
            // Given
            String content = "Hey @john, please review this task with @jane";
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.parseMentionsAndResolve(content, organizationId, null);

            // Then
            assertThat(userIds).containsExactlyInAnyOrder(user1Id, user2Id);
        }

        @Test
        @DisplayName("Should exclude author from resolved mentions")
        void shouldExcludeAuthorFromResolvedMentions() {
            // Given
            String content = "I (@john) and @jane will handle this.";
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

            // When
            Set<UUID> userIds = mentionService.parseMentionsAndResolve(content, organizationId, user1Id);

            // Then
            assertThat(userIds).containsExactly(user2Id);
        }
    }

    @Nested
    @DisplayName("Get Mentionable Users")
    class GetMentionableUsersTests {

        @Test
        @DisplayName("Should return all mentionable users in organization")
        void shouldReturnAllMentionableUsers() {
            // Given
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2, member3));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
            when(userRepository.findById(user3Id)).thenReturn(Optional.of(user3));

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).hasSize(3);
        }

        @Test
        @DisplayName("Should return users sorted by display name")
        void shouldReturnUsersSortedByDisplayName() {
            // Given
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1, member2, member3));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
            when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
            when(userRepository.findById(user3Id)).thenReturn(Optional.of(user3));

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).hasSize(3);
            assertThat(users.get(0).getDisplayName()).isEqualTo("Bob Wilson");
            assertThat(users.get(1).getDisplayName()).isEqualTo("Jane Smith");
            assertThat(users.get(2).getDisplayName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should include mention name in response")
        void shouldIncludeMentionNameInResponse() {
            // Given
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getMentionName()).isEqualTo("john.doe");
        }

        @Test
        @DisplayName("Should include profile photo URL in response")
        void shouldIncludeProfilePhotoUrlInResponse() {
            // Given
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getProfilePhotoUrl()).isEqualTo("https://example.com/john.jpg");
        }

        @Test
        @DisplayName("Should return empty list when no members")
        void shouldReturnEmptyListWhenNoMembers() {
            // Given
            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(Collections.emptyList());

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).isEmpty();
        }

        @Test
        @DisplayName("Should handle user with only first name")
        void shouldHandleUserWithOnlyFirstName() {
            // Given
            User userWithOnlyFirstName = User.builder()
                    .id(user1Id)
                    .email("john@example.com")
                    .firstName("John")
                    .lastName(null)
                    .build();

            when(memberRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(member1));
            when(userRepository.findById(user1Id)).thenReturn(Optional.of(userWithOnlyFirstName));

            // When
            List<MentionService.MentionableUser> users = mentionService.getMentionableUsers(organizationId);

            // Then
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getMentionName()).isEqualTo("john");
        }
    }
}
