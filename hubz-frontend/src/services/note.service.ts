import api from './api';
import type {
  Note,
  CreateNoteRequest,
  UpdateNoteRequest,
  NoteFolder,
  CreateNoteFolderRequest,
  UpdateNoteFolderRequest,
  NoteTag,
  CreateNoteTagRequest,
  UpdateNoteTagRequest,
  NoteVersion,
} from '../types/note';

export const noteService = {
  // Notes
  async getByOrganization(
    organizationId: string,
    options?: { category?: string; folderId?: string; rootOnly?: boolean }
  ): Promise<Note[]> {
    const params = new URLSearchParams();
    if (options?.category) params.append('category', options.category);
    if (options?.folderId) params.append('folderId', options.folderId);
    if (options?.rootOnly) params.append('rootOnly', 'true');

    const response = await api.get<Note[]>(
      `/organizations/${organizationId}/notes${params.toString() ? `?${params.toString()}` : ''}`
    );
    return response.data;
  },

  async getById(id: string): Promise<Note> {
    const response = await api.get<Note>(`/notes/${id}`);
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

  async moveToFolder(noteId: string, folderId?: string): Promise<Note> {
    const params = folderId ? `?folderId=${folderId}` : '';
    const response = await api.patch<Note>(`/notes/${noteId}/folder${params}`);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/notes/${id}`);
  },

  async search(organizationId: string, query: string): Promise<Note[]> {
    const response = await api.get<Note[]>(
      `/organizations/${organizationId}/notes/search?q=${encodeURIComponent(query)}`
    );
    return response.data;
  },
};

export const noteFolderService = {
  async getByOrganization(organizationId: string, flat = false): Promise<NoteFolder[]> {
    const params = flat ? '?flat=true' : '';
    const response = await api.get<NoteFolder[]>(
      `/organizations/${organizationId}/note-folders${params}`
    );
    return response.data;
  },

  async getById(id: string): Promise<NoteFolder> {
    const response = await api.get<NoteFolder>(`/note-folders/${id}`);
    return response.data;
  },

  async create(organizationId: string, data: CreateNoteFolderRequest): Promise<NoteFolder> {
    const response = await api.post<NoteFolder>(
      `/organizations/${organizationId}/note-folders`,
      data
    );
    return response.data;
  },

  async update(id: string, data: UpdateNoteFolderRequest): Promise<NoteFolder> {
    const response = await api.put<NoteFolder>(`/note-folders/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/note-folders/${id}`);
  },
};

export const noteTagService = {
  async getByOrganization(organizationId: string): Promise<NoteTag[]> {
    const response = await api.get<NoteTag[]>(`/organizations/${organizationId}/note-tags`);
    return response.data;
  },

  async getById(id: string): Promise<NoteTag> {
    const response = await api.get<NoteTag>(`/note-tags/${id}`);
    return response.data;
  },

  async create(organizationId: string, data: CreateNoteTagRequest): Promise<NoteTag> {
    const response = await api.post<NoteTag>(`/organizations/${organizationId}/note-tags`, data);
    return response.data;
  },

  async update(id: string, data: UpdateNoteTagRequest): Promise<NoteTag> {
    const response = await api.put<NoteTag>(`/note-tags/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/note-tags/${id}`);
  },

  async getTagsByNote(noteId: string): Promise<NoteTag[]> {
    const response = await api.get<NoteTag[]>(`/notes/${noteId}/tags`);
    return response.data;
  },

  async addTagToNote(noteId: string, tagId: string): Promise<void> {
    await api.post(`/notes/${noteId}/tags/${tagId}`);
  },

  async removeTagFromNote(noteId: string, tagId: string): Promise<void> {
    await api.delete(`/notes/${noteId}/tags/${tagId}`);
  },

  async setNoteTags(noteId: string, tagIds: string[]): Promise<void> {
    await api.put(`/notes/${noteId}/tags`, tagIds);
  },
};

export const noteVersionService = {
  async getVersions(noteId: string): Promise<NoteVersion[]> {
    const response = await api.get<NoteVersion[]>(`/notes/${noteId}/versions`);
    return response.data;
  },

  async getVersion(noteId: string, versionId: string): Promise<NoteVersion> {
    const response = await api.get<NoteVersion>(`/notes/${noteId}/versions/${versionId}`);
    return response.data;
  },

  async restoreVersion(noteId: string, versionId: string): Promise<NoteVersion> {
    const response = await api.post<NoteVersion>(`/notes/${noteId}/restore/${versionId}`);
    return response.data;
  },
};
