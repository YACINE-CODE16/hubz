export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  description: string | null;
  profilePhotoUrl: string | null;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
  oauthProvider: string | null;
  createdAt: string;
}

export interface DeleteAccountRequest {
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  totpCode?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  description?: string;
}

export interface AuthResponse {
  token: string | null;
  user: User | null;
  requires2FA: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface MessageResponse {
  message: string;
  success: boolean;
}

// Two-Factor Authentication types
export interface TwoFactorSetupResponse {
  secret: string;
  qrCodeImage: string;
  otpAuthUri: string;
}

export interface TwoFactorVerifyRequest {
  code: string;
}

export interface TwoFactorDisableRequest {
  password: string;
  code: string;
}

export interface TwoFactorStatusResponse {
  enabled: boolean;
  message: string;
}
