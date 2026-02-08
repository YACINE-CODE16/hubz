package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistProgressResponse {
    private UUID taskId;
    private int totalItems;
    private int completedItems;
    private double completionPercentage;
    private List<ChecklistItemResponse> items;
}
