import api from './api';
import type { Insight } from '../types/insight';

export const insightService = {
  async getInsights(): Promise<Insight[]> {
    const response = await api.get<Insight[]>('/users/me/insights');
    return response.data;
  },
};
