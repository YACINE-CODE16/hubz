package com.hubz.application.service;

import com.hubz.application.dto.response.SearchResultResponse;
import com.hubz.application.dto.response.SearchResultResponse.EventSearchResult;
import com.hubz.application.dto.response.SearchResultResponse.GoalSearchResult;
import com.hubz.application.dto.response.SearchResultResponse.MemberSearchResult;
import com.hubz.application.dto.response.SearchResultResponse.NoteSearchResult;
import com.hubz.application.dto.response.SearchResultResponse.OrganizationSearchResult;
import com.hubz.application.dto.response.SearchResultResponse.TaskSearchResult;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final OrganizationRepositoryPort organizationRepository;
    private final OrganizationMemberRepositoryPort memberRepository;
    private final TaskRepositoryPort taskRepository;
    private final GoalRepositoryPort goalRepository;
    private final EventRepositoryPort eventRepository;
    private final NoteRepositoryPort noteRepository;
    private final UserRepositoryPort userRepository;

    private static final int MAX_RESULTS_PER_CATEGORY = 10;

    public SearchResultResponse search(String query, UUID userId) {
        if (query == null || query.trim().isEmpty()) {
            return SearchResultResponse.builder()
                    .organizations(List.of())
                    .tasks(List.of())
                    .goals(List.of())
                    .events(List.of())
                    .notes(List.of())
                    .members(List.of())
                    .totalResults(0)
                    .build();
        }

        String normalizedQuery = query.trim().toLowerCase();

        // Get user's organization IDs for filtering results
        List<UUID> userOrgIds = memberRepository.findByUserId(userId).stream()
                .map(OrganizationMember::getOrganizationId)
                .toList();

        // Build organization name map for lookups
        Map<UUID, String> orgNames = organizationRepository.findAll().stream()
                .filter(org -> userOrgIds.contains(org.getId()))
                .collect(Collectors.toMap(Organization::getId, Organization::getName));

        // Search organizations
        List<OrganizationSearchResult> organizations = searchOrganizations(normalizedQuery, userOrgIds);

        // Search tasks
        List<TaskSearchResult> tasks = searchTasks(normalizedQuery, userOrgIds, orgNames);

        // Search goals
        List<GoalSearchResult> goals = searchGoals(normalizedQuery, userOrgIds, userId, orgNames);

        // Search events
        List<EventSearchResult> events = searchEvents(normalizedQuery, userOrgIds, userId, orgNames);

        // Search notes
        List<NoteSearchResult> notes = searchNotes(normalizedQuery, userOrgIds, orgNames);

        // Search members
        List<MemberSearchResult> members = searchMembers(normalizedQuery, userOrgIds, orgNames);

        int totalResults = organizations.size() + tasks.size() + goals.size() +
                events.size() + notes.size() + members.size();

        return SearchResultResponse.builder()
                .organizations(organizations)
                .tasks(tasks)
                .goals(goals)
                .events(events)
                .notes(notes)
                .members(members)
                .totalResults(totalResults)
                .build();
    }

    private List<OrganizationSearchResult> searchOrganizations(String query, List<UUID> userOrgIds) {
        return organizationRepository.searchByName(query).stream()
                .filter(org -> userOrgIds.contains(org.getId()))
                .limit(MAX_RESULTS_PER_CATEGORY)
                .map(org -> OrganizationSearchResult.builder()
                        .id(org.getId().toString())
                        .name(org.getName())
                        .description(org.getDescription())
                        .icon(org.getIcon())
                        .color(org.getColor())
                        .matchedField("name")
                        .build())
                .toList();
    }

    private List<TaskSearchResult> searchTasks(String query, List<UUID> orgIds, Map<UUID, String> orgNames) {
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return taskRepository.searchByTitleOrDescription(query, orgIds).stream()
                .limit(MAX_RESULTS_PER_CATEGORY)
                .map(task -> {
                    String matchedField = containsIgnoreCase(task.getTitle(), query) ? "title" : "description";
                    return TaskSearchResult.builder()
                            .id(task.getId().toString())
                            .title(task.getTitle())
                            .description(task.getDescription())
                            .status(task.getStatus().name())
                            .priority(task.getPriority().name())
                            .organizationId(task.getOrganizationId().toString())
                            .organizationName(orgNames.get(task.getOrganizationId()))
                            .matchedField(matchedField)
                            .build();
                })
                .toList();
    }

    private List<GoalSearchResult> searchGoals(String query, List<UUID> orgIds, UUID userId, Map<UUID, String> orgNames) {
        return goalRepository.searchByTitle(query, orgIds, userId).stream()
                .limit(MAX_RESULTS_PER_CATEGORY)
                .map(goal -> GoalSearchResult.builder()
                        .id(goal.getId().toString())
                        .title(goal.getTitle())
                        .description(goal.getDescription())
                        .type(goal.getType().name())
                        .deadline(goal.getDeadline() != null ? goal.getDeadline().toString() : null)
                        .organizationId(goal.getOrganizationId() != null ? goal.getOrganizationId().toString() : null)
                        .organizationName(goal.getOrganizationId() != null ? orgNames.get(goal.getOrganizationId()) : "Personal")
                        .matchedField("title")
                        .build())
                .toList();
    }

    private List<EventSearchResult> searchEvents(String query, List<UUID> orgIds, UUID userId, Map<UUID, String> orgNames) {
        return eventRepository.searchByTitleOrDescription(query, orgIds, userId).stream()
                .limit(MAX_RESULTS_PER_CATEGORY)
                .map(event -> {
                    String matchedField = containsIgnoreCase(event.getTitle(), query) ? "title" : "description";
                    return EventSearchResult.builder()
                            .id(event.getId().toString())
                            .title(event.getTitle())
                            .description(event.getDescription())
                            .startTime(event.getStartTime() != null ? event.getStartTime().toString() : null)
                            .endTime(event.getEndTime() != null ? event.getEndTime().toString() : null)
                            .organizationId(event.getOrganizationId() != null ? event.getOrganizationId().toString() : null)
                            .organizationName(event.getOrganizationId() != null ? orgNames.get(event.getOrganizationId()) : "Personal")
                            .matchedField(matchedField)
                            .build();
                })
                .toList();
    }

    private List<NoteSearchResult> searchNotes(String query, List<UUID> orgIds, Map<UUID, String> orgNames) {
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return noteRepository.searchByTitleOrContent(query, orgIds).stream()
                .limit(MAX_RESULTS_PER_CATEGORY)
                .map(note -> {
                    String matchedField = containsIgnoreCase(note.getTitle(), query) ? "title" : "content";
                    return NoteSearchResult.builder()
                            .id(note.getId().toString())
                            .title(note.getTitle())
                            .content(truncateContent(note.getContent(), 100))
                            .category(note.getCategory())
                            .organizationId(note.getOrganizationId().toString())
                            .organizationName(orgNames.get(note.getOrganizationId()))
                            .matchedField(matchedField)
                            .build();
                })
                .toList();
    }

    private List<MemberSearchResult> searchMembers(String query, List<UUID> userOrgIds, Map<UUID, String> orgNames) {
        List<MemberSearchResult> results = new ArrayList<>();

        for (UUID orgId : userOrgIds) {
            List<OrganizationMember> members = memberRepository.findByOrganizationId(orgId);
            for (OrganizationMember member : members) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user != null) {
                    String fullName = user.getFirstName() + " " + user.getLastName();
                    String matchedField = null;
                    if (containsIgnoreCase(fullName, query)) {
                        matchedField = "name";
                    } else if (containsIgnoreCase(user.getEmail(), query)) {
                        matchedField = "email";
                    }

                    if (matchedField != null) {
                        results.add(MemberSearchResult.builder()
                                .id(member.getId().toString())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .organizationId(orgId.toString())
                                .organizationName(orgNames.get(orgId))
                                .role(member.getRole().name())
                                .matchedField(matchedField)
                                .build());
                    }
                }
            }

            if (results.size() >= MAX_RESULTS_PER_CATEGORY) {
                break;
            }
        }

        return results.stream().limit(MAX_RESULTS_PER_CATEGORY).toList();
    }

    private boolean containsIgnoreCase(String str, String query) {
        if (str == null) return false;
        return str.toLowerCase().contains(query.toLowerCase());
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
}
