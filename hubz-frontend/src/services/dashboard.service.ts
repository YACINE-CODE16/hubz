import api from './api';
import type { PersonalDashboard } from '../types/dashboard';

export const dashboardService = {
  async getPersonalDashboard(): Promise<PersonalDashboard> {
    const response = await api.get<PersonalDashboard>('/users/me/dashboard');
    return response.data;
  },
};
