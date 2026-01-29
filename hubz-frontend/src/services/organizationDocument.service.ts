import api from './api';
import type { OrganizationDocument } from '../types/organizationDocument';

export const organizationDocumentService = {
  async uploadDocument(organizationId: string, file: File): Promise<OrganizationDocument> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<OrganizationDocument>(
      `/organizations/${organizationId}/documents`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  async getDocuments(organizationId: string): Promise<OrganizationDocument[]> {
    const response = await api.get<OrganizationDocument[]>(
      `/organizations/${organizationId}/documents`
    );
    return response.data;
  },

  async downloadDocument(documentId: string): Promise<Blob> {
    const response = await api.get(`/organizations/documents/${documentId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async deleteDocument(documentId: string): Promise<void> {
    await api.delete(`/organizations/documents/${documentId}`);
  },

  getDownloadUrl(documentId: string): string {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/organizations/documents/${documentId}/download`;
  },
};
