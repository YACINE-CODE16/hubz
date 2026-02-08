import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAuthStore } from './authStore';
import type { User } from '../types/auth';

// Mock the auth.service to prevent actual API calls
vi.mock('../services/auth.service', () => ({
  authService: {
    getCurrentUser: vi.fn(),
  },
}));

describe('authStore', () => {
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
    // Reset store to initial state before each test
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
    });
  });

  it('should have correct initial state', () => {
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('should set auth with token and user', () => {
    useAuthStore.getState().setAuth('jwt-token-123', mockUser);

    const state = useAuthStore.getState();
    expect(state.token).toBe('jwt-token-123');
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
  });

  it('should update user without changing token', () => {
    useAuthStore.getState().setAuth('jwt-token-123', mockUser);

    const updatedUser: User = {
      ...mockUser,
      firstName: 'Jane',
      lastName: 'Smith',
    };
    useAuthStore.getState().setUser(updatedUser);

    const state = useAuthStore.getState();
    expect(state.user?.firstName).toBe('Jane');
    expect(state.user?.lastName).toBe('Smith');
    expect(state.token).toBe('jwt-token-123');
    expect(state.isAuthenticated).toBe(true);
  });

  it('should clear all auth state on logout', () => {
    // First, set authenticated state
    useAuthStore.getState().setAuth('jwt-token-123', mockUser);
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    // Logout
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('should validate session successfully', async () => {
    // Set up initial auth state
    useAuthStore.getState().setAuth('jwt-token-123', mockUser);

    // Mock the dynamic import of authService
    const { authService } = await import('../services/auth.service');
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce(mockUser);

    await useAuthStore.getState().validateSession();

    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
  });

  it('should logout on failed session validation', async () => {
    // Set up initial auth state
    useAuthStore.getState().setAuth('expired-token', mockUser);

    // Mock the dynamic import of authService to throw
    const { authService } = await import('../services/auth.service');
    vi.mocked(authService.getCurrentUser).mockRejectedValueOnce(new Error('Unauthorized'));

    await useAuthStore.getState().validateSession();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('should preserve token when updating user', () => {
    useAuthStore.getState().setAuth('my-token', mockUser);
    useAuthStore.getState().setUser({ ...mockUser, description: 'Updated description' });

    const state = useAuthStore.getState();
    expect(state.token).toBe('my-token');
    expect(state.user?.description).toBe('Updated description');
  });
});
