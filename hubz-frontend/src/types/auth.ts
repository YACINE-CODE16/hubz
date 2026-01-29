export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  description: string | null;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  description?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
