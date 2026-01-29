import api from './api';
import type { Team, TeamMember, CreateTeamRequest, UpdateTeamRequest } from '../types/team';

export const teamService = {
  async getByOrganization(organizationId: string): Promise<Team[]> {
    const response = await api.get<Team[]>(`/organizations/${organizationId}/teams`);
    return response.data;
  },

  async create(organizationId: string, data: CreateTeamRequest): Promise<Team> {
    const response = await api.post<Team>(`/organizations/${organizationId}/teams`, data);
    return response.data;
  },

  async update(id: string, data: UpdateTeamRequest): Promise<Team> {
    const response = await api.put<Team>(`/teams/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/teams/${id}`);
  },

  async getMembers(teamId: string): Promise<TeamMember[]> {
    const response = await api.get<TeamMember[]>(`/teams/${teamId}/members`);
    return response.data;
  },

  async addMember(teamId: string, userId: string): Promise<void> {
    await api.post(`/teams/${teamId}/members/${userId}`);
  },

  async removeMember(teamId: string, userId: string): Promise<void> {
    await api.delete(`/teams/${teamId}/members/${userId}`);
  },
};
