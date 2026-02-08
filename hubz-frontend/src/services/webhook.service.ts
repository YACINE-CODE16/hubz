import api from './api';
import type {
  WebhookConfig,
  CreateWebhookConfigRequest,
  UpdateWebhookConfigRequest,
  WebhookTestResponse,
} from '../types/webhook';

export const webhookService = {
  async getByOrganization(orgId: string): Promise<WebhookConfig[]> {
    const response = await api.get<WebhookConfig[]>(
      `/organizations/${orgId}/webhooks`
    );
    return response.data;
  },

  async getById(orgId: string, id: string): Promise<WebhookConfig> {
    const response = await api.get<WebhookConfig>(
      `/organizations/${orgId}/webhooks/${id}`
    );
    return response.data;
  },

  async create(
    orgId: string,
    data: CreateWebhookConfigRequest
  ): Promise<WebhookConfig> {
    const response = await api.post<WebhookConfig>(
      `/organizations/${orgId}/webhooks`,
      data
    );
    return response.data;
  },

  async update(
    orgId: string,
    id: string,
    data: UpdateWebhookConfigRequest
  ): Promise<WebhookConfig> {
    const response = await api.put<WebhookConfig>(
      `/organizations/${orgId}/webhooks/${id}`,
      data
    );
    return response.data;
  },

  async delete(orgId: string, id: string): Promise<void> {
    await api.delete(`/organizations/${orgId}/webhooks/${id}`);
  },

  async test(orgId: string, id: string): Promise<WebhookTestResponse> {
    const response = await api.post<WebhookTestResponse>(
      `/organizations/${orgId}/webhooks/${id}/test`
    );
    return response.data;
  },
};
