import api from './api';
import type {
  ChatMessage,
  ChatMessagePage,
  CreateMessageRequest,
  UpdateMessageRequest,
} from '../types/chat';

export const teamChatService = {
  async getMessages(
    teamId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ChatMessagePage> {
    const response = await api.get<ChatMessagePage>(
      `/teams/${teamId}/messages?page=${page}&size=${size}`
    );
    return response.data;
  },

  async sendMessage(
    teamId: string,
    data: CreateMessageRequest
  ): Promise<ChatMessage> {
    const response = await api.post<ChatMessage>(
      `/teams/${teamId}/messages`,
      data
    );
    return response.data;
  },

  async editMessage(
    teamId: string,
    messageId: string,
    data: UpdateMessageRequest
  ): Promise<ChatMessage> {
    const response = await api.put<ChatMessage>(
      `/teams/${teamId}/messages/${messageId}`,
      data
    );
    return response.data;
  },

  async deleteMessage(teamId: string, messageId: string): Promise<void> {
    await api.delete(`/teams/${teamId}/messages/${messageId}`);
  },

  async getMessageCount(teamId: string): Promise<number> {
    const response = await api.get<number>(
      `/teams/${teamId}/messages/count`
    );
    return response.data;
  },
};
