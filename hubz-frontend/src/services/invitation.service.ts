import api from './api';
import type { Invitation, CreateInvitationRequest, InvitationInfo } from '../types/invitation';

export const invitationService = {
  async createInvitation(organizationId: string, data: CreateInvitationRequest): Promise<Invitation> {
    const response = await api.post<Invitation>(
      `/organizations/${organizationId}/invitations`,
      data
    );
    return response.data;
  },

  async getInvitations(organizationId: string): Promise<Invitation[]> {
    const response = await api.get<Invitation[]>(`/organizations/${organizationId}/invitations`);
    return response.data;
  },

  async getInvitationInfo(token: string): Promise<InvitationInfo> {
    const response = await api.get<InvitationInfo>(`/invitations/${token}/info`);
    return response.data;
  },

  async acceptInvitation(token: string): Promise<void> {
    await api.post(`/invitations/${token}/accept`);
  },

  async deleteInvitation(invitationId: string): Promise<void> {
    await api.delete(`/invitations/${invitationId}`);
  },

  getInvitationLink(token: string): string {
    return `${window.location.origin}/join/${token}`;
  },
};
