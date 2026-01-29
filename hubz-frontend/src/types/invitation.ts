export interface Invitation {
  id: string;
  organizationId: string;
  email: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
  token: string;
  invitationUrl: string;
  createdBy: string;
  createdAt: string;
  expiresAt: string;
  used: boolean;
  acceptedBy?: string;
  acceptedAt?: string;
}

export interface CreateInvitationRequest {
  email: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
}

export interface InvitationInfo {
  organizationId: string;
  email: string;
  role: string;
  expiresAt: string;
  used: boolean;
}
