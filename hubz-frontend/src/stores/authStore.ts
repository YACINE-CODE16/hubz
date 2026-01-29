import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types/auth';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  validateSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setAuth: (token, user) => set({ token, user, isAuthenticated: true }),
      logout: () => set({ token: null, user: null, isAuthenticated: false }),
      validateSession: async () => {
        try {
          // Import dynamically to avoid circular dependency
          const { authService } = await import('../services/auth.service');
          const user = await authService.getCurrentUser();
          set({ user, isAuthenticated: true });
        } catch {
          get().logout();
        }
      },
    }),
    { name: 'hubz-auth' },
  ),
);
