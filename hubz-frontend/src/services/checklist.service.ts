import api from './api';
import type {
  ChecklistItem,
  ChecklistProgress,
  CreateChecklistItemRequest,
  UpdateChecklistItemRequest,
  ReorderChecklistItemsRequest,
} from '../types/task';

export const checklistService = {
  async getChecklist(taskId: string): Promise<ChecklistProgress> {
    const response = await api.get<ChecklistProgress>(`/tasks/${taskId}/checklist`);
    return response.data;
  },

  async createItem(taskId: string, data: CreateChecklistItemRequest): Promise<ChecklistItem> {
    const response = await api.post<ChecklistItem>(`/tasks/${taskId}/checklist`, data);
    return response.data;
  },

  async updateItem(taskId: string, itemId: string, data: UpdateChecklistItemRequest): Promise<ChecklistItem> {
    const response = await api.put<ChecklistItem>(`/tasks/${taskId}/checklist/${itemId}`, data);
    return response.data;
  },

  async toggleItem(taskId: string, itemId: string): Promise<ChecklistItem> {
    const response = await api.patch<ChecklistItem>(`/tasks/${taskId}/checklist/${itemId}/toggle`);
    return response.data;
  },

  async reorderItems(taskId: string, data: ReorderChecklistItemsRequest): Promise<ChecklistItem[]> {
    const response = await api.put<ChecklistItem[]>(`/tasks/${taskId}/checklist/reorder`, data);
    return response.data;
  },

  async deleteItem(taskId: string, itemId: string): Promise<void> {
    await api.delete(`/tasks/${taskId}/checklist/${itemId}`);
  },
};
