export type WebhookServiceType = 'SLACK' | 'DISCORD' | 'GITHUB' | 'CUSTOM';

export type WebhookEventType =
  | 'TASK_CREATED'
  | 'TASK_COMPLETED'
  | 'GOAL_COMPLETED'
  | 'NOTE_CREATED'
  | 'MEMBER_JOINED';

export interface WebhookConfig {
  id: string;
  organizationId: string;
  service: WebhookServiceType;
  webhookUrl: string;
  name: string;
  hasSecret: boolean;
  events: WebhookEventType[];
  enabled: boolean;
  createdById: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWebhookConfigRequest {
  service: WebhookServiceType;
  webhookUrl: string;
  name: string;
  secret?: string;
  events: WebhookEventType[];
}

export interface UpdateWebhookConfigRequest {
  service?: WebhookServiceType;
  webhookUrl?: string;
  name?: string;
  secret?: string;
  events?: WebhookEventType[];
  enabled?: boolean;
}

export interface WebhookTestResponse {
  success: boolean;
  statusCode: number;
  message: string;
}
