export interface Note {
  id: string;
  title: string;
  content: string;
  category?: string;
  organizationId: string;
  createdById: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNoteRequest {
  title: string;
  content: string;
  category?: string;
}

export interface UpdateNoteRequest {
  title: string;
  content: string;
  category?: string;
}
