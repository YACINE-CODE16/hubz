import api from './api';
import type { TaskHistory, TaskHistoryField } from '../types/task';

export const taskHistoryService = {
  async getTaskHistory(taskId: string, field?: TaskHistoryField): Promise<TaskHistory[]> {
    const params = field ? { field } : {};
    const response = await api.get<TaskHistory[]>(`/tasks/${taskId}/history`, { params });
    return response.data;
  },
};
