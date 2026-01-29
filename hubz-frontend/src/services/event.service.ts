import api from './api';
import type {
  Event,
  CreateEventRequest,
  UpdateEventRequest,
} from '../types/event';

export const eventService = {
  async getByOrganization(
    organizationId: string,
    start?: string,
    end?: string
  ): Promise<Event[]> {
    const params = new URLSearchParams();
    if (start) params.append('start', start);
    if (end) params.append('end', end);

    const response = await api.get<Event[]>(
      `/organizations/${organizationId}/events${params.toString() ? `?${params.toString()}` : ''}`
    );
    return response.data;
  },

  async createOrganizationEvent(
    organizationId: string,
    data: CreateEventRequest
  ): Promise<Event> {
    const response = await api.post<Event>(
      `/organizations/${organizationId}/events`,
      data
    );
    return response.data;
  },

  async getPersonalEvents(start?: string, end?: string): Promise<Event[]> {
    const params = new URLSearchParams();
    if (start) params.append('start', start);
    if (end) params.append('end', end);

    const response = await api.get<Event[]>(
      `/users/me/events${params.toString() ? `?${params.toString()}` : ''}`
    );
    return response.data;
  },

  async createPersonalEvent(data: CreateEventRequest): Promise<Event> {
    const response = await api.post<Event>('/users/me/events', data);
    return response.data;
  },

  async update(id: string, data: UpdateEventRequest): Promise<Event> {
    const response = await api.put<Event>(`/events/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/events/${id}`);
  },
};
