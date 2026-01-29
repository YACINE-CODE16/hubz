import api from './api';
import type { Note, CreateNoteRequest, UpdateNoteRequest } from '../types/note';

export const noteService = {
  async getByOrganization(organizationId: string, category?: string): Promise<Note[]> {
    const params = new URLSearchParams();
    if (category) params.append('category', category);

    const response = await api.get<Note[]>(
      `/organizations/${organizationId}/notes${params.toString() ? `?${params.toString()}` : ''}`
    );
    return response.data;
  },

  async create(organizationId: string, data: CreateNoteRequest): Promise<Note> {
    const response = await api.post<Note>(`/organizations/${organizationId}/notes`, data);
    return response.data;
  },

  async update(id: string, data: UpdateNoteRequest): Promise<Note> {
    const response = await api.put<Note>(`/notes/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/notes/${id}`);
  },
};
