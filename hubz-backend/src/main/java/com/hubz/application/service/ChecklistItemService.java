package com.hubz.application.service;

import com.hubz.application.dto.request.CreateChecklistItemRequest;
import com.hubz.application.dto.request.ReorderChecklistItemsRequest;
import com.hubz.application.dto.request.UpdateChecklistItemRequest;
import com.hubz.application.dto.response.ChecklistItemResponse;
import com.hubz.application.dto.response.ChecklistProgressResponse;
import com.hubz.application.port.out.ChecklistItemRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.exception.ChecklistItemNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.ChecklistItem;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistItemService {

    private final ChecklistItemRepositoryPort checklistRepository;
    private final TaskRepositoryPort taskRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public ChecklistItemResponse create(UUID taskId, CreateChecklistItemRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        int position = request.getPosition() != null
                ? request.getPosition()
                : checklistRepository.getMaxPositionByTaskId(taskId) + 1;

        ChecklistItem item = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content(request.getContent())
                .completed(false)
                .position(position)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(checklistRepository.save(item));
    }

    public ChecklistProgressResponse getChecklist(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        List<ChecklistItem> items = checklistRepository.findByTaskIdOrderByPosition(taskId);
        int totalItems = items.size();
        int completedItems = (int) items.stream().filter(ChecklistItem::isCompleted).count();

        double completionPercentage = totalItems > 0
                ? (double) completedItems / totalItems * 100
                : 0;

        return ChecklistProgressResponse.builder()
                .taskId(taskId)
                .totalItems(totalItems)
                .completedItems(completedItems)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .items(items.stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional
    public ChecklistItemResponse update(UUID itemId, UpdateChecklistItemRequest request, UUID currentUserId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));

        Task task = taskRepository.findById(item.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(item.getTaskId()));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        if (request.getContent() != null) {
            item.setContent(request.getContent());
        }
        if (request.getCompleted() != null) {
            item.setCompleted(request.getCompleted());
        }
        if (request.getPosition() != null) {
            item.setPosition(request.getPosition());
        }
        item.setUpdatedAt(LocalDateTime.now());

        return toResponse(checklistRepository.save(item));
    }

    @Transactional
    public ChecklistItemResponse toggleCompleted(UUID itemId, UUID currentUserId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));

        Task task = taskRepository.findById(item.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(item.getTaskId()));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        item.setCompleted(!item.isCompleted());
        item.setUpdatedAt(LocalDateTime.now());

        return toResponse(checklistRepository.save(item));
    }

    @Transactional
    public List<ChecklistItemResponse> reorder(UUID taskId, ReorderChecklistItemsRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        List<ChecklistItem> items = checklistRepository.findByTaskId(taskId);
        Map<UUID, ChecklistItem> itemMap = items.stream()
                .collect(Collectors.toMap(ChecklistItem::getId, Function.identity()));

        List<ChecklistItem> updatedItems = new ArrayList<>();
        int position = 0;

        for (UUID itemId : request.getItemIds()) {
            ChecklistItem item = itemMap.get(itemId);
            if (item != null) {
                item.setPosition(position++);
                item.setUpdatedAt(LocalDateTime.now());
                updatedItems.add(item);
            }
        }

        return checklistRepository.saveAll(updatedItems).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID itemId, UUID currentUserId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));

        Task task = taskRepository.findById(item.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(item.getTaskId()));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        checklistRepository.deleteById(itemId);
    }

    private ChecklistItemResponse toResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTaskId())
                .content(item.getContent())
                .completed(item.isCompleted())
                .position(item.getPosition())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
