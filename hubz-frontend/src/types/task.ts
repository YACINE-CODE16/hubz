import type { Tag } from './tag';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority | null;
  organizationId: string | null;
  goalId: string | null;
  assigneeId: string | null;
  creatorId: string;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority?: TaskPriority;
  goalId?: string;
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  goalId?: string;
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus;
}

export interface TaskComment {
  id: string;
  taskId: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId: string | null;
  replies: TaskComment[];
  createdAt: string;
  updatedAt: string;
  edited: boolean;
}

export interface CreateTaskCommentRequest {
  content: string;
  parentCommentId?: string;
}

export interface UpdateTaskCommentRequest {
  content: string;
}

// Checklist types
export interface ChecklistItem {
  id: string;
  taskId: string;
  content: string;
  completed: boolean;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChecklistProgress {
  taskId: string;
  totalItems: number;
  completedItems: number;
  completionPercentage: number;
  items: ChecklistItem[];
}

export interface CreateChecklistItemRequest {
  content: string;
  position?: number;
}

export interface UpdateChecklistItemRequest {
  content?: string;
  completed?: boolean;
  position?: number;
}

export interface ReorderChecklistItemsRequest {
  itemIds: string[];
}

// Task Attachment types
export interface TaskAttachment {
  id: string;
  taskId: string;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedAt: string;
}

// Task History types
export type TaskHistoryField =
  | 'TITLE'
  | 'DESCRIPTION'
  | 'STATUS'
  | 'PRIORITY'
  | 'ASSIGNEE'
  | 'DUE_DATE'
  | 'GOAL';

export interface TaskHistory {
  id: string;
  taskId: string;
  userId: string;
  userName: string;
  userPhotoUrl: string | null;
  fieldChanged: TaskHistoryField;
  oldValue: string | null;
  newValue: string | null;
  changedAt: string;
}
