import api from './api';
import { useAuthStore } from '../stores/authStore';
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResendVerificationRequest,
  ResetPasswordRequest,
  TwoFactorDisableRequest,
  TwoFactorSetupResponse,
  TwoFactorStatusResponse,
  TwoFactorVerifyRequest,
  User,
} from '../types/auth';

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', data);
    // Only set auth if we got a token (not when 2FA is required)
    if (response.data.token && response.data.user) {
      useAuthStore.getState().setAuth(response.data.token, response.data.user);
    }
    return response.data;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', data);
    if (response.data.token && response.data.user) {
      useAuthStore.getState().setAuth(response.data.token, response.data.user);
    }
    return response.data;
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  logout() {
    useAuthStore.getState().logout();
  },

  // Password reset
  async forgotPassword(data: ForgotPasswordRequest): Promise<MessageResponse> {
    const response = await api.post<MessageResponse>('/auth/forgot-password', data);
    return response.data;
  },

  async resetPassword(data: ResetPasswordRequest): Promise<MessageResponse> {
    const response = await api.post<MessageResponse>('/auth/reset-password', data);
    return response.data;
  },

  async checkResetTokenValid(token: string): Promise<MessageResponse> {
    const response = await api.get<MessageResponse>(`/auth/reset-password/${token}/valid`);
    return response.data;
  },

  // Email verification
  async verifyEmail(token: string): Promise<MessageResponse> {
    const response = await api.get<MessageResponse>(`/auth/verify-email/${token}`);
    return response.data;
  },

  async resendVerification(data: ResendVerificationRequest): Promise<MessageResponse> {
    const response = await api.post<MessageResponse>('/auth/resend-verification', data);
    return response.data;
  },

  async checkVerificationTokenValid(token: string): Promise<MessageResponse> {
    const response = await api.get<MessageResponse>(`/auth/verify-email/${token}/valid`);
    return response.data;
  },

  // Two-Factor Authentication (2FA)
  async setup2FA(): Promise<TwoFactorSetupResponse> {
    const response = await api.post<TwoFactorSetupResponse>('/auth/2fa/setup');
    return response.data;
  },

  async verify2FA(data: TwoFactorVerifyRequest): Promise<TwoFactorStatusResponse> {
    const response = await api.post<TwoFactorStatusResponse>('/auth/2fa/verify', data);
    return response.data;
  },

  async disable2FA(data: TwoFactorDisableRequest): Promise<TwoFactorStatusResponse> {
    const response = await api.delete<TwoFactorStatusResponse>('/auth/2fa/disable', {
      data,
    });
    return response.data;
  },

  async get2FAStatus(): Promise<TwoFactorStatusResponse> {
    const response = await api.get<TwoFactorStatusResponse>('/auth/2fa/status');
    return response.data;
  },

  // OAuth2 Google
  getGoogleOAuthUrl(): string {
    return 'http://localhost:8085/api/auth/oauth2/google';
  },

  /**
   * Handle the OAuth callback by fetching the current user with the received token.
   * The token is provided via URL query parameter after Google callback redirect.
   */
  async handleOAuthCallback(token: string): Promise<void> {
    // Fetch the current user using the token from the OAuth redirect
    const response = await api.get<User>('/auth/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
    useAuthStore.getState().setAuth(token, response.data);
  },
};
