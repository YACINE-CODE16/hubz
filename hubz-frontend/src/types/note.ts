export interface NoteTag {
  id: string;
  name: string;
  color: string;
  organizationId: string;
  createdAt: string;
}

export interface NoteFolder {
  id: string;
  name: string;
  parentFolderId?: string;
  organizationId: string;
  createdById: string;
  createdAt: string;
  updatedAt: string;
  children?: NoteFolder[];
  noteCount?: number;
}

export interface Note {
  id: string;
  title: string;
  content: string;
  category?: string;
  folderId?: string;
  organizationId: string;
  createdById: string;
  createdAt: string;
  updatedAt: string;
  tags?: NoteTag[];
}

export interface CreateNoteRequest {
  title: string;
  content: string;
  category?: string;
  folderId?: string;
  tagIds?: string[];
}

export interface UpdateNoteRequest {
  title: string;
  content: string;
  category?: string;
  folderId?: string;
  tagIds?: string[];
}

export interface CreateNoteFolderRequest {
  name: string;
  parentFolderId?: string;
}

export interface UpdateNoteFolderRequest {
  name?: string;
  parentFolderId?: string;
}

export interface CreateNoteTagRequest {
  name: string;
  color: string;
}

export interface UpdateNoteTagRequest {
  name?: string;
  color?: string;
}

export interface NoteVersion {
  id: string;
  noteId: string;
  versionNumber: number;
  title: string;
  content: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
}
