import api from './api';
import type { Notification, NotificationCountResponse, NotificationPreferences, UpdateNotificationPreferencesRequest } from '../types/notification';

export const notificationService = {
  async getNotifications(limit: number = 50): Promise<Notification[]> {
    const response = await api.get<Notification[]>('/notifications', {
      params: { limit },
    });
    return response.data;
  },

  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await api.get<Notification[]>('/notifications/unread');
    return response.data;
  },

  async getUnreadCount(): Promise<NotificationCountResponse> {
    const response = await api.get<NotificationCountResponse>('/notifications/count');
    return response.data;
  },

  async markAsRead(notificationId: string): Promise<void> {
    await api.post(`/notifications/${notificationId}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await api.post('/notifications/read-all');
  },

  async deleteNotification(notificationId: string): Promise<void> {
    await api.delete(`/notifications/${notificationId}`);
  },

  async deleteAllNotifications(): Promise<void> {
    await api.delete('/notifications');
  },

  async getPreferences(): Promise<NotificationPreferences> {
    const response = await api.get<NotificationPreferences>('/notifications/preferences');
    return response.data;
  },

  async updatePreferences(request: UpdateNotificationPreferencesRequest): Promise<NotificationPreferences> {
    const response = await api.put<NotificationPreferences>('/notifications/preferences', request);
    return response.data;
  },
};
