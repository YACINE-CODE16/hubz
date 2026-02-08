import api from './api';
import type { UserPreferences, UpdatePreferencesRequest } from '../types/preferences';

export const preferencesService = {
  /**
   * Get current user's preferences.
   * If no preferences exist, returns default values.
   */
  getPreferences: async (): Promise<UserPreferences> => {
    const response = await api.get<UserPreferences>('/users/me/preferences');
    return response.data;
  },

  /**
   * Update current user's preferences.
   */
  updatePreferences: async (request: UpdatePreferencesRequest): Promise<UserPreferences> => {
    const response = await api.put<UserPreferences>('/users/me/preferences', request);
    return response.data;
  },

  /**
   * Get list of supported timezones from the API.
   */
  getSupportedTimezones: async (): Promise<string[]> => {
    const response = await api.get<string[]>('/users/me/preferences/timezones');
    return response.data;
  },
};
