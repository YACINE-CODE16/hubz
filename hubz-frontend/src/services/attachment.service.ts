import api from './api';
import type { NoteAttachment } from '../types/attachment';

export const attachmentService = {
  async uploadAttachment(noteId: string, file: File): Promise<NoteAttachment> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<NoteAttachment>(`/notes/${noteId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  async getAttachments(noteId: string): Promise<NoteAttachment[]> {
    const response = await api.get<NoteAttachment[]>(`/notes/${noteId}/attachments`);
    return response.data;
  },

  async downloadAttachment(attachmentId: string): Promise<Blob> {
    const response = await api.get(`/attachments/${attachmentId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async deleteAttachment(attachmentId: string): Promise<void> {
    await api.delete(`/attachments/${attachmentId}`);
  },

  getDownloadUrl(attachmentId: string): string {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/attachments/${attachmentId}/download`;
  },
};
