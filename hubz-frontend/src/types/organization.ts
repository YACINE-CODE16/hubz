export interface Organization {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  color: string | null;
  readme: string | null;
  logoUrl: string | null;
  ownerId: string;
  createdAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  icon?: string;
  color?: string;
}

export interface UpdateOrganizationRequest {
  name?: string;
  description?: string;
  icon?: string;
  color?: string;
  readme?: string;
}

export type MemberRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface Member {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  profilePhotoUrl: string | null;
  role: MemberRole;
  joinedAt: string;
}
