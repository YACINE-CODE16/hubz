export interface NoteCollaborator {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  initials: string;
  profilePhotoUrl?: string;
  color: string;
  joinedAt: string;
  lastActiveAt: string;
}

export interface NoteCursor {
  userId: string;
  email: string;
  displayName: string;
  color: string;
  position: number;
  selectionStart?: number;
  selectionEnd?: number;
}

export interface NoteSession {
  noteId: string;
  organizationId: string;
  currentTitle: string;
  currentContent: string;
  version: number;
  lastModifiedAt: string;
  collaborators: NoteCollaborator[];
  cursors: NoteCursor[];
}

export type EditType = 'TITLE_UPDATE' | 'CONTENT_UPDATE' | 'FULL_UPDATE';

export interface NoteEdit {
  noteId: string;
  userId: string;
  email: string;
  displayName: string;
  type: EditType;
  title?: string;
  content?: string;
  version: number;
  timestamp: string;
  hasConflict: boolean;
  conflictMessage?: string;
}

export type CollaborationEventType = 'USER_JOINED' | 'USER_LEFT' | 'USER_TYPING' | 'USER_STOPPED_TYPING';

export interface NoteCollaborationEvent {
  eventType: CollaborationEventType;
  noteId: string;
  collaborator: NoteCollaborator;
  totalCollaborators: number;
  timestamp: string;
}

export interface NoteJoinRequest {
  noteId: string;
}

export interface NoteLeaveRequest {
  noteId: string;
}

export interface NoteEditRequest {
  noteId: string;
  type: EditType;
  title?: string;
  content?: string;
  baseVersion: number;
}

export interface NoteCursorRequest {
  noteId: string;
  position: number;
  selectionStart?: number;
  selectionEnd?: number;
}

export interface WebSocketMessage<T> {
  type: string;
  payload: T;
}
