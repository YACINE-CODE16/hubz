import api from './api';
import type { ChatMessageRequest, ChatbotResponse } from '../types/chatbot';

export const chatbotService = {
  async sendMessage(request: ChatMessageRequest): Promise<ChatbotResponse> {
    const response = await api.post<ChatbotResponse>('/chatbot/message', request);
    return response.data;
  },
};
