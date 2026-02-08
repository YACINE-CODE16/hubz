import api from './api';
import type { MentionableUser } from '../types/mention';

export const mentionService = {
  /**
   * Get all mentionable users in an organization.
   * Used for the @mention autocomplete in comments.
   */
  async getMentionableUsers(organizationId: string): Promise<MentionableUser[]> {
    const response = await api.get<MentionableUser[]>(
      `/organizations/${organizationId}/mentions/users`
    );
    return response.data;
  },
};
