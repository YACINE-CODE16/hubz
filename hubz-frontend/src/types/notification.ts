export type NotificationType =
  | 'TASK_ASSIGNED'
  | 'TASK_COMPLETED'
  | 'TASK_DUE_SOON'
  | 'TASK_OVERDUE'
  | 'ORGANIZATION_INVITE'
  | 'ORGANIZATION_ROLE_CHANGED'
  | 'ORGANIZATION_MEMBER_JOINED'
  | 'ORGANIZATION_MEMBER_LEFT'
  | 'GOAL_DEADLINE_APPROACHING'
  | 'GOAL_COMPLETED'
  | 'GOAL_AT_RISK'
  | 'EVENT_REMINDER'
  | 'EVENT_UPDATED'
  | 'EVENT_CANCELLED'
  | 'MENTION'
  | 'SYSTEM';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  link: string | null;
  referenceId: string | null;
  organizationId: string | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationCountResponse {
  unreadCount: number;
}
