import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from './auth.service';
import { useAuthStore } from '../stores/authStore';
import type { AuthResponse, User } from '../types/auth';

// Mock the api module
vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  },
}));

// Import the mocked api after mock declaration
import api from './api';

describe('authService', () => {
  const mockUser: User = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    description: null,
    profilePhotoUrl: null,
    emailVerified: true,
    twoFactorEnabled: false,
    createdAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    // Reset auth store
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
    });
  });

  describe('login', () => {
    it('should login and set auth state on success', async () => {
      const authResponse: AuthResponse = {
        token: 'jwt-token-123',
        user: mockUser,
        requires2FA: false,
      };
      vi.mocked(api.post).mockResolvedValueOnce({ data: authResponse });

      const result = await authService.login({ email: 'test@example.com', password: 'password' });

      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: 'password',
      });
      expect(result).toEqual(authResponse);
      expect(useAuthStore.getState().token).toBe('jwt-token-123');
      expect(useAuthStore.getState().user).toEqual(mockUser);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
    });

    it('should not set auth state when 2FA is required', async () => {
      const authResponse: AuthResponse = {
        token: null,
        user: null,
        requires2FA: true,
      };
      vi.mocked(api.post).mockResolvedValueOnce({ data: authResponse });

      const result = await authService.login({ email: 'test@example.com', password: 'password' });

      expect(result.requires2FA).toBe(true);
      expect(useAuthStore.getState().token).toBeNull();
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });

    it('should throw error on invalid credentials', async () => {
      vi.mocked(api.post).mockRejectedValueOnce(new Error('Invalid credentials'));

      await expect(
        authService.login({ email: 'bad@example.com', password: 'wrong' })
      ).rejects.toThrow('Invalid credentials');
    });
  });

  describe('register', () => {
    it('should register and set auth state on success', async () => {
      const authResponse: AuthResponse = {
        token: 'new-jwt-token',
        user: mockUser,
        requires2FA: false,
      };
      vi.mocked(api.post).mockResolvedValueOnce({ data: authResponse });

      const result = await authService.register({
        email: 'new@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      });

      expect(api.post).toHaveBeenCalledWith('/auth/register', {
        email: 'new@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      });
      expect(result).toEqual(authResponse);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
    });
  });

  describe('getCurrentUser', () => {
    it('should return current user', async () => {
      vi.mocked(api.get).mockResolvedValueOnce({ data: mockUser });

      const result = await authService.getCurrentUser();

      expect(api.get).toHaveBeenCalledWith('/auth/me');
      expect(result).toEqual(mockUser);
    });
  });

  describe('logout', () => {
    it('should clear auth state', () => {
      useAuthStore.getState().setAuth('token', mockUser);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);

      authService.logout();

      expect(useAuthStore.getState().token).toBeNull();
      expect(useAuthStore.getState().user).toBeNull();
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });

  describe('forgotPassword', () => {
    it('should send forgot password request', async () => {
      const response = { message: 'Email sent', success: true };
      vi.mocked(api.post).mockResolvedValueOnce({ data: response });

      const result = await authService.forgotPassword({ email: 'test@example.com' });

      expect(api.post).toHaveBeenCalledWith('/auth/forgot-password', {
        email: 'test@example.com',
      });
      expect(result).toEqual(response);
    });
  });

  describe('resetPassword', () => {
    it('should send reset password request', async () => {
      const response = { message: 'Password reset', success: true };
      vi.mocked(api.post).mockResolvedValueOnce({ data: response });

      const result = await authService.resetPassword({
        token: 'reset-token',
        newPassword: 'newPassword123',
      });

      expect(api.post).toHaveBeenCalledWith('/auth/reset-password', {
        token: 'reset-token',
        newPassword: 'newPassword123',
      });
      expect(result).toEqual(response);
    });
  });

  describe('checkResetTokenValid', () => {
    it('should check if reset token is valid', async () => {
      const response = { message: 'Valid', success: true };
      vi.mocked(api.get).mockResolvedValueOnce({ data: response });

      const result = await authService.checkResetTokenValid('my-token');

      expect(api.get).toHaveBeenCalledWith('/auth/reset-password/my-token/valid');
      expect(result).toEqual(response);
    });
  });

  describe('verifyEmail', () => {
    it('should verify email with token', async () => {
      const response = { message: 'Email verified', success: true };
      vi.mocked(api.get).mockResolvedValueOnce({ data: response });

      const result = await authService.verifyEmail('verify-token');

      expect(api.get).toHaveBeenCalledWith('/auth/verify-email/verify-token');
      expect(result).toEqual(response);
    });
  });

  describe('2FA methods', () => {
    it('should setup 2FA', async () => {
      const setupResponse = { secret: 'TOTP_SECRET', qrCodeImage: 'base64...', otpAuthUri: 'otpauth://...' };
      vi.mocked(api.post).mockResolvedValueOnce({ data: setupResponse });

      const result = await authService.setup2FA();

      expect(api.post).toHaveBeenCalledWith('/auth/2fa/setup');
      expect(result).toEqual(setupResponse);
    });

    it('should verify 2FA code', async () => {
      const response = { enabled: true, message: '2FA enabled' };
      vi.mocked(api.post).mockResolvedValueOnce({ data: response });

      const result = await authService.verify2FA({ code: '123456' });

      expect(api.post).toHaveBeenCalledWith('/auth/2fa/verify', { code: '123456' });
      expect(result).toEqual(response);
    });

    it('should disable 2FA', async () => {
      const response = { enabled: false, message: '2FA disabled' };
      vi.mocked(api.delete).mockResolvedValueOnce({ data: response });

      const result = await authService.disable2FA({ password: 'pass', code: '123456' });

      expect(api.delete).toHaveBeenCalledWith('/auth/2fa/disable', {
        data: { password: 'pass', code: '123456' },
      });
      expect(result).toEqual(response);
    });

    it('should get 2FA status', async () => {
      const response = { enabled: true, message: '2FA is active' };
      vi.mocked(api.get).mockResolvedValueOnce({ data: response });

      const result = await authService.get2FAStatus();

      expect(api.get).toHaveBeenCalledWith('/auth/2fa/status');
      expect(result).toEqual(response);
    });
  });
});
