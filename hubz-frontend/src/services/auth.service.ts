import api from './api';
import { useAuthStore } from '../stores/authStore';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types/auth';

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', data);
    useAuthStore.getState().setAuth(response.data.token, response.data.user);
    return response.data;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', data);
    useAuthStore.getState().setAuth(response.data.token, response.data.user);
    return response.data;
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  logout() {
    useAuthStore.getState().logout();
  },
};
