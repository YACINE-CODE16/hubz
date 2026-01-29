import api from './api';
import type {
  CreateTaskRequest,
  Task,
  UpdateTaskRequest,
  UpdateTaskStatusRequest,
} from '../types/task';

export const taskService = {
  async getByOrganization(orgId: string): Promise<Task[]> {
    const response = await api.get<Task[]>(`/organizations/${orgId}/tasks`);
    return response.data;
  },

  async create(orgId: string, data: CreateTaskRequest): Promise<Task> {
    const response = await api.post<Task>(`/organizations/${orgId}/tasks`, data);
    return response.data;
  },

  async update(id: string, data: UpdateTaskRequest): Promise<Task> {
    const response = await api.put<Task>(`/tasks/${id}`, data);
    return response.data;
  },

  async updateStatus(id: string, data: UpdateTaskStatusRequest): Promise<Task> {
    const response = await api.patch<Task>(`/tasks/${id}/status`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/tasks/${id}`);
  },

  async getMyTasks(): Promise<Task[]> {
    const response = await api.get<Task[]>('/users/me/tasks');
    return response.data;
  },
};
