export interface OrganizationDocument {
  id: string;
  organizationId: string;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedAt: string;
}
