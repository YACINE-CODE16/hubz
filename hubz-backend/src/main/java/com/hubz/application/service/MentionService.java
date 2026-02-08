package com.hubz.application.service;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing and resolving @mentions in text content.
 * Supports mentions by firstName, lastName, or full name (firstName.lastName).
 */
@Service
@RequiredArgsConstructor
public class MentionService {

    private final UserRepositoryPort userRepository;
    private final OrganizationMemberRepositoryPort memberRepository;

    // Pattern to match @mentions: @word or @word.word (for firstName.lastName)
    // Captures the mention text after @ (without the @ symbol)
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z]+(?:\\.[a-zA-Z]+)?)");

    /**
     * Parse the content and extract all @mentions.
     *
     * @param content the text content to parse
     * @return set of mention strings (without the @ symbol)
     */
    public Set<String> extractMentions(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> mentions = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            mentions.add(matcher.group(1).toLowerCase());
        }

        return mentions;
    }

    /**
     * Resolve mentions to user IDs within an organization context.
     * Only resolves mentions for users who are members of the organization.
     *
     * @param mentions set of mention strings to resolve
     * @param organizationId the organization context
     * @param excludeUserId optional user ID to exclude (e.g., the author of the comment)
     * @return set of resolved user IDs
     */
    public Set<UUID> resolveMentionsToUserIds(Set<String> mentions, UUID organizationId, UUID excludeUserId) {
        if (mentions.isEmpty()) {
            return Collections.emptySet();
        }

        // Get all members of the organization
        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);

        // Build a map of mention patterns to user IDs
        Map<String, UUID> mentionToUserId = new HashMap<>();

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }

            String firstName = user.getFirstName() != null ? user.getFirstName().toLowerCase() : "";
            String lastName = user.getLastName() != null ? user.getLastName().toLowerCase() : "";

            // Add possible mention patterns
            if (!firstName.isEmpty()) {
                mentionToUserId.put(firstName, user.getId());
            }
            if (!lastName.isEmpty()) {
                mentionToUserId.put(lastName, user.getId());
            }
            if (!firstName.isEmpty() && !lastName.isEmpty()) {
                mentionToUserId.put(firstName + "." + lastName, user.getId());
            }
        }

        // Resolve mentions to user IDs
        Set<UUID> resolvedUserIds = mentions.stream()
                .map(String::toLowerCase)
                .map(mentionToUserId::get)
                .filter(Objects::nonNull)
                .filter(userId -> excludeUserId == null || !userId.equals(excludeUserId))
                .collect(Collectors.toSet());

        return resolvedUserIds;
    }

    /**
     * Parse content and resolve all mentions to user IDs.
     * Convenience method that combines extractMentions and resolveMentionsToUserIds.
     *
     * @param content the text content to parse
     * @param organizationId the organization context
     * @param excludeUserId optional user ID to exclude
     * @return set of resolved user IDs
     */
    public Set<UUID> parseMentionsAndResolve(String content, UUID organizationId, UUID excludeUserId) {
        Set<String> mentions = extractMentions(content);
        return resolveMentionsToUserIds(mentions, organizationId, excludeUserId);
    }

    /**
     * Get all members of an organization that can be mentioned.
     * Returns a list of mentionable users with their mention patterns.
     *
     * @param organizationId the organization ID
     * @return list of mentionable members
     */
    public List<MentionableUser> getMentionableUsers(UUID organizationId) {
        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);

        return members.stream()
                .map(member -> userRepository.findById(member.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .map(user -> MentionableUser.builder()
                        .userId(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .displayName(user.getFirstName() + " " + user.getLastName())
                        .mentionName(buildMentionName(user))
                        .profilePhotoUrl(user.getProfilePhotoUrl())
                        .build())
                .sorted(Comparator.comparing(MentionableUser::getDisplayName))
                .collect(Collectors.toList());
    }

    private String buildMentionName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName.toLowerCase() + "." + lastName.toLowerCase();
        } else if (!firstName.isEmpty()) {
            return firstName.toLowerCase();
        } else if (!lastName.isEmpty()) {
            return lastName.toLowerCase();
        }
        return "";
    }

    /**
     * DTO representing a user that can be mentioned.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MentionableUser {
        private UUID userId;
        private String firstName;
        private String lastName;
        private String displayName;
        private String mentionName;
        private String profilePhotoUrl;
    }
}
