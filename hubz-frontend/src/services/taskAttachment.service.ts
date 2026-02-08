import api from './api';
import type { TaskAttachment } from '../types/task';

export const taskAttachmentService = {
  async uploadAttachment(taskId: string, file: File): Promise<TaskAttachment> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<TaskAttachment>(`/tasks/${taskId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  async getAttachments(taskId: string): Promise<TaskAttachment[]> {
    const response = await api.get<TaskAttachment[]>(`/tasks/${taskId}/attachments`);
    return response.data;
  },

  async getAttachmentCount(taskId: string): Promise<number> {
    const response = await api.get<{ count: number }>(`/tasks/${taskId}/attachments/count`);
    return response.data.count;
  },

  async downloadAttachment(attachmentId: string): Promise<Blob> {
    const response = await api.get(`/task-attachments/${attachmentId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async deleteAttachment(attachmentId: string): Promise<void> {
    await api.delete(`/task-attachments/${attachmentId}`);
  },

  getDownloadUrl(attachmentId: string): string {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/task-attachments/${attachmentId}/download`;
  },

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  },

  getFileIcon(contentType: string): string {
    if (contentType.startsWith('image/')) return 'image';
    if (contentType === 'application/pdf') return 'file-text';
    if (contentType.includes('spreadsheet') || contentType.includes('excel')) return 'table';
    if (contentType.includes('document') || contentType.includes('word')) return 'file-text';
    if (contentType.includes('presentation') || contentType.includes('powerpoint')) return 'presentation';
    if (contentType.startsWith('video/')) return 'video';
    if (contentType.startsWith('audio/')) return 'music';
    if (contentType.includes('zip') || contentType.includes('compressed')) return 'archive';
    return 'file';
  },
};
