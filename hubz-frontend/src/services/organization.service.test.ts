import { describe, it, expect, vi, beforeEach } from 'vitest';
import { organizationService } from './organization.service';
import type { Organization, Member } from '../types/organization';

// Mock the api module
vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

import api from './api';

describe('organizationService', () => {
  const mockOrganization: Organization = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    name: 'Test Org',
    description: 'A test organization',
    icon: 'building',
    color: '#3B82F6',
    readme: null,
    logoUrl: null,
    ownerId: '456e4567-e89b-12d3-a456-426614174000',
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockMember: Member = {
    id: 'member-1',
    userId: 'user-1',
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane@example.com',
    profilePhotoUrl: null,
    role: 'MEMBER',
    joinedAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all organizations', async () => {
      const orgs = [mockOrganization];
      vi.mocked(api.get).mockResolvedValueOnce({ data: orgs });

      const result = await organizationService.getAll();

      expect(api.get).toHaveBeenCalledWith('/organizations');
      expect(result).toEqual(orgs);
    });

    it('should return empty array when no organizations', async () => {
      vi.mocked(api.get).mockResolvedValueOnce({ data: [] });

      const result = await organizationService.getAll();

      expect(result).toEqual([]);
    });
  });

  describe('getById', () => {
    it('should fetch organization by id', async () => {
      vi.mocked(api.get).mockResolvedValueOnce({ data: mockOrganization });

      const result = await organizationService.getById('123');

      expect(api.get).toHaveBeenCalledWith('/organizations/123');
      expect(result).toEqual(mockOrganization);
    });
  });

  describe('create', () => {
    it('should create a new organization', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({ data: mockOrganization });

      const result = await organizationService.create({
        name: 'Test Org',
        description: 'A test organization',
      });

      expect(api.post).toHaveBeenCalledWith('/organizations', {
        name: 'Test Org',
        description: 'A test organization',
      });
      expect(result).toEqual(mockOrganization);
    });
  });

  describe('update', () => {
    it('should update an organization', async () => {
      const updated = { ...mockOrganization, name: 'Updated Org' };
      vi.mocked(api.put).mockResolvedValueOnce({ data: updated });

      const result = await organizationService.update('123', { name: 'Updated Org' });

      expect(api.put).toHaveBeenCalledWith('/organizations/123', { name: 'Updated Org' });
      expect(result.name).toBe('Updated Org');
    });
  });

  describe('delete', () => {
    it('should delete an organization', async () => {
      vi.mocked(api.delete).mockResolvedValueOnce({ data: undefined });

      await organizationService.delete('123');

      expect(api.delete).toHaveBeenCalledWith('/organizations/123');
    });
  });

  describe('getMembers', () => {
    it('should fetch members of an organization', async () => {
      const members = [mockMember];
      vi.mocked(api.get).mockResolvedValueOnce({ data: members });

      const result = await organizationService.getMembers('org-123');

      expect(api.get).toHaveBeenCalledWith('/organizations/org-123/members');
      expect(result).toEqual(members);
    });
  });

  describe('changeMemberRole', () => {
    it('should change a member role', async () => {
      const updatedMember = { ...mockMember, role: 'ADMIN' as const };
      vi.mocked(api.patch).mockResolvedValueOnce({ data: updatedMember });

      const result = await organizationService.changeMemberRole('org-123', 'user-1', 'ADMIN');

      expect(api.patch).toHaveBeenCalledWith(
        '/organizations/org-123/members/user-1/role',
        { role: 'ADMIN' }
      );
      expect(result.role).toBe('ADMIN');
    });
  });

  describe('removeMember', () => {
    it('should remove a member from organization', async () => {
      vi.mocked(api.delete).mockResolvedValueOnce({ data: undefined });

      await organizationService.removeMember('org-123', 'user-1');

      expect(api.delete).toHaveBeenCalledWith('/organizations/org-123/members/user-1');
    });
  });

  describe('transferOwnership', () => {
    it('should transfer ownership to another user', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({ data: undefined });

      await organizationService.transferOwnership('org-123', 'new-owner-id');

      expect(api.post).toHaveBeenCalledWith(
        '/organizations/org-123/transfer-ownership/new-owner-id'
      );
    });
  });

  describe('uploadLogo', () => {
    it('should upload organization logo', async () => {
      const updatedOrg = { ...mockOrganization, logoUrl: 'logos/logo.png' };
      vi.mocked(api.post).mockResolvedValueOnce({ data: updatedOrg });

      const file = new File(['logo'], 'logo.png', { type: 'image/png' });
      const result = await organizationService.uploadLogo('org-123', file);

      expect(api.post).toHaveBeenCalledWith(
        '/organizations/org-123/logo',
        expect.any(FormData),
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      expect(result.logoUrl).toBe('logos/logo.png');
    });
  });

  describe('deleteLogo', () => {
    it('should delete organization logo', async () => {
      const updatedOrg = { ...mockOrganization, logoUrl: null };
      vi.mocked(api.delete).mockResolvedValueOnce({ data: updatedOrg });

      const result = await organizationService.deleteLogo('org-123');

      expect(api.delete).toHaveBeenCalledWith('/organizations/org-123/logo');
      expect(result.logoUrl).toBeNull();
    });
  });
});
