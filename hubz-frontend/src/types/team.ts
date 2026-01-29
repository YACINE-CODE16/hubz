export interface Team {
  id: string;
  name: string;
  description?: string;
  organizationId: string;
  createdAt: string;
  memberCount: number;
}

export interface TeamMember {
  id: string;
  teamId: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  joinedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface UpdateTeamRequest {
  name: string;
  description?: string;
}
