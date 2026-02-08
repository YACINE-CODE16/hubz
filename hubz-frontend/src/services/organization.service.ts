import api from './api';
import type {
  CreateOrganizationRequest,
  Organization,
  UpdateOrganizationRequest,
  Member,
  MemberRole,
} from '../types/organization';

export const organizationService = {
  async getAll(): Promise<Organization[]> {
    const response = await api.get<Organization[]>('/organizations');
    return response.data;
  },

  async create(data: CreateOrganizationRequest): Promise<Organization> {
    const response = await api.post<Organization>('/organizations', data);
    return response.data;
  },

  async update(id: string, data: UpdateOrganizationRequest): Promise<Organization> {
    const response = await api.put<Organization>(`/organizations/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/organizations/${id}`);
  },

  async getMembers(organizationId: string): Promise<Member[]> {
    const response = await api.get<Member[]>(`/organizations/${organizationId}/members`);
    return response.data;
  },

  async changeMemberRole(
    organizationId: string,
    userId: string,
    role: MemberRole
  ): Promise<Member> {
    const response = await api.patch<Member>(
      `/organizations/${organizationId}/members/${userId}/role`,
      { role }
    );
    return response.data;
  },

  async removeMember(organizationId: string, userId: string): Promise<void> {
    await api.delete(`/organizations/${organizationId}/members/${userId}`);
  },

  async transferOwnership(organizationId: string, newOwnerId: string): Promise<void> {
    await api.post(`/organizations/${organizationId}/transfer-ownership/${newOwnerId}`);
  },

  async uploadLogo(organizationId: string, file: File): Promise<Organization> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<Organization>(
      `/organizations/${organizationId}/logo`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  async deleteLogo(organizationId: string): Promise<Organization> {
    const response = await api.delete<Organization>(`/organizations/${organizationId}/logo`);
    return response.data;
  },

  async getById(organizationId: string): Promise<Organization> {
    const response = await api.get<Organization>(`/organizations/${organizationId}`);
    return response.data;
  },
};
