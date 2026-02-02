import api from './api';
import type {
  TaskComment,
  CreateTaskCommentRequest,
  UpdateTaskCommentRequest,
} from '../types/task';

export const commentService = {
  async getByTask(taskId: string): Promise<TaskComment[]> {
    const response = await api.get<TaskComment[]>(`/tasks/${taskId}/comments`);
    return response.data;
  },

  async getCount(taskId: string): Promise<number> {
    const response = await api.get<{ count: number }>(`/tasks/${taskId}/comments/count`);
    return response.data.count;
  },

  async create(taskId: string, data: CreateTaskCommentRequest): Promise<TaskComment> {
    const response = await api.post<TaskComment>(`/tasks/${taskId}/comments`, data);
    return response.data;
  },

  async update(taskId: string, commentId: string, data: UpdateTaskCommentRequest): Promise<TaskComment> {
    const response = await api.put<TaskComment>(`/tasks/${taskId}/comments/${commentId}`, data);
    return response.data;
  },

  async delete(taskId: string, commentId: string): Promise<void> {
    await api.delete(`/tasks/${taskId}/comments/${commentId}`);
  },
};
