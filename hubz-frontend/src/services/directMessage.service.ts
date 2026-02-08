import api from './api';
import type {
  Conversation,
  DirectMessage,
  PageResponse,
  SendDirectMessageRequest,
  UnreadCountResponse,
  UpdateDirectMessageRequest,
} from '../types/directMessage';

export const directMessageService = {
  async sendMessage(data: SendDirectMessageRequest): Promise<DirectMessage> {
    const response = await api.post<DirectMessage>('/messages', data);
    return response.data;
  },

  async getConversations(): Promise<Conversation[]> {
    const response = await api.get<Conversation[]>('/messages/conversations');
    return response.data;
  },

  async getConversation(
    userId: string,
    page = 0,
    size = 50,
  ): Promise<PageResponse<DirectMessage>> {
    const response = await api.get<PageResponse<DirectMessage>>(
      `/messages/conversation/${userId}`,
      { params: { page, size } },
    );
    return response.data;
  },

  async editMessage(messageId: string, data: UpdateDirectMessageRequest): Promise<DirectMessage> {
    const response = await api.put<DirectMessage>(`/messages/${messageId}`, data);
    return response.data;
  },

  async deleteMessage(messageId: string): Promise<void> {
    await api.delete(`/messages/${messageId}`);
  },

  async markAsRead(messageId: string): Promise<void> {
    await api.post(`/messages/${messageId}/read`);
  },

  async markConversationAsRead(userId: string): Promise<void> {
    await api.post(`/messages/conversation/${userId}/read`);
  },

  async getUnreadCount(): Promise<UnreadCountResponse> {
    const response = await api.get<UnreadCountResponse>('/messages/unread/count');
    return response.data;
  },
};
