import api from './api';
import type { OrganizationDocument, DocumentPreviewResponse, DocumentVersion } from '../types/organizationDocument';

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

  async getDocumentPreview(documentId: string): Promise<DocumentPreviewResponse> {
    const response = await api.get<DocumentPreviewResponse>(
      `/organizations/documents/${documentId}/preview`
    );
    return response.data;
  },

  getPreviewContentUrl(documentId: string): string {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/organizations/documents/${documentId}/preview/content`;
  },

  // Document Versioning

  async getDocumentVersions(documentId: string): Promise<DocumentVersion[]> {
    const response = await api.get<DocumentVersion[]>(
      `/organizations/documents/${documentId}/versions`
    );
    return response.data;
  },

  async uploadNewVersion(documentId: string, file: File): Promise<DocumentVersion> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<DocumentVersion>(
      `/organizations/documents/${documentId}/versions`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  async downloadDocumentVersion(documentId: string, versionNumber: number): Promise<Blob> {
    const response = await api.get(
      `/organizations/documents/${documentId}/versions/${versionNumber}/download`,
      {
        responseType: 'blob',
      }
    );
    return response.data;
  },

  getVersionDownloadUrl(documentId: string, versionNumber: number): string {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/organizations/documents/${documentId}/versions/${versionNumber}/download`;
  },
};
