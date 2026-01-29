import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/auth.service';
import type { LoginRequest, RegisterRequest } from '../types/auth';

export function useAuth() {
  const { user, isAuthenticated } = useAuthStore();

  return {
    user,
    isAuthenticated,
    login: (data: LoginRequest) => authService.login(data),
    register: (data: RegisterRequest) => authService.register(data),
    logout: () => authService.logout(),
  };
}
