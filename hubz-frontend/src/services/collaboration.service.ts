import api from './api';
import type { NoteCollaborator, NoteSession } from '../types/collaboration';

const BASE_URL = '/api/notes';

export const collaborationService = {
  /**
   * Get collaborators for a note (REST endpoint)
   */
  async getCollaborators(noteId: string): Promise<NoteCollaborator[]> {
    const response = await api.get<NoteCollaborator[]>(`${BASE_URL}/${noteId}/collaborators`);
    return response.data;
  },

  /**
   * Get collaborator count for a note (REST endpoint)
   */
  async getCollaboratorCount(noteId: string): Promise<number> {
    const response = await api.get<number>(`${BASE_URL}/${noteId}/collaborators/count`);
    return response.data;
  },

  /**
   * Get current session for a note (REST endpoint)
   */
  async getSession(noteId: string): Promise<NoteSession | null> {
    try {
      const response = await api.get<NoteSession>(`${BASE_URL}/${noteId}/session`);
      return response.data;
    } catch (error) {
      // 404 means no active session
      return null;
    }
  },
};
