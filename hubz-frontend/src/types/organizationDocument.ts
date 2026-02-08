import type { Tag } from './tag';

export interface OrganizationDocument {
  id: string;
  organizationId: string;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedAt: string;
  tags: Tag[];
  currentVersionNumber?: number;
  totalVersions?: number;
}

export interface DocumentPreviewResponse {
  id: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  previewable: boolean;
  previewType: 'image' | 'pdf' | 'text' | 'unsupported';
  textContent: string | null;
}

export interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedByName: string;
  uploadedAt: string;
}
