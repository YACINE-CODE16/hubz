package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    private List<OrganizationSearchResult> organizations;
    private List<TaskSearchResult> tasks;
    private List<GoalSearchResult> goals;
    private List<EventSearchResult> events;
    private List<NoteSearchResult> notes;
    private List<MemberSearchResult> members;
    private int totalResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationSearchResult {
        private String id;
        private String name;
        private String description;
        private String icon;
        private String color;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSearchResult {
        private String id;
        private String title;
        private String description;
        private String status;
        private String priority;
        private String organizationId;
        private String organizationName;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalSearchResult {
        private String id;
        private String title;
        private String description;
        private String type;
        private String deadline;
        private String organizationId;
        private String organizationName;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSearchResult {
        private String id;
        private String title;
        private String description;
        private String startTime;
        private String endTime;
        private String organizationId;
        private String organizationName;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteSearchResult {
        private String id;
        private String title;
        private String content;
        private String category;
        private String organizationId;
        private String organizationName;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSearchResult {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String organizationId;
        private String organizationName;
        private String role;
        private String matchedField;
    }
}
