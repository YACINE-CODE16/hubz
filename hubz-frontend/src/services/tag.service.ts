import api from './api';
import type { Tag, CreateTagRequest, UpdateTagRequest } from '../types/tag';

export const tagService = {
  async getByOrganization(orgId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/organizations/${orgId}/tags`);
    return response.data;
  },

  async create(orgId: string, data: CreateTagRequest): Promise<Tag> {
    const response = await api.post<Tag>(`/organizations/${orgId}/tags`, data);
    return response.data;
  },

  async getById(id: string): Promise<Tag> {
    const response = await api.get<Tag>(`/tags/${id}`);
    return response.data;
  },

  async update(id: string, data: UpdateTagRequest): Promise<Tag> {
    const response = await api.put<Tag>(`/tags/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/tags/${id}`);
  },

  // Task-Tag operations
  async getTagsByTask(taskId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/tasks/${taskId}/tags`);
    return response.data;
  },

  async addTagToTask(taskId: string, tagId: string): Promise<void> {
    await api.post(`/tasks/${taskId}/tags/${tagId}`);
  },

  async removeTagFromTask(taskId: string, tagId: string): Promise<void> {
    await api.delete(`/tasks/${taskId}/tags/${tagId}`);
  },

  async setTaskTags(taskId: string, tagIds: string[]): Promise<void> {
    await api.put(`/tasks/${taskId}/tags`, tagIds);
  },

  // Document-Tag operations
  async getTagsByDocument(documentId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/documents/${documentId}/tags`);
    return response.data;
  },

  async addTagToDocument(documentId: string, tagId: string): Promise<void> {
    await api.post(`/documents/${documentId}/tags/${tagId}`);
  },

  async removeTagFromDocument(documentId: string, tagId: string): Promise<void> {
    await api.delete(`/documents/${documentId}/tags/${tagId}`);
  },

  async setDocumentTags(documentId: string, tagIds: string[]): Promise<void> {
    await api.put(`/documents/${documentId}/tags`, tagIds);
  },
};
