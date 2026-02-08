import api from './api';
import { useAuthStore } from '../stores/authStore';
import type { DeleteAccountRequest, User } from '../types/auth';

export const userService = {
  /**
   * Upload or update profile photo.
   * @param file The photo file to upload
   * @returns The updated user with new profile photo URL
   */
  async uploadProfilePhoto(file: File): Promise<User> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<User>('/users/me/photo', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    // Update the auth store with the new user data
    const token = useAuthStore.getState().token;
    if (token) {
      useAuthStore.getState().setAuth(token, response.data);
    }

    return response.data;
  },

  /**
   * Delete the current user's profile photo.
   * @returns The updated user without profile photo
   */
  async deleteProfilePhoto(): Promise<User> {
    const response = await api.delete<User>('/users/me/photo');

    // Update the auth store with the new user data
    const token = useAuthStore.getState().token;
    if (token) {
      useAuthStore.getState().setAuth(token, response.data);
    }

    return response.data;
  },

  /**
   * Delete the current user's account.
   * Requires password confirmation.
   * @param data The delete account request with password
   */
  async deleteAccount(data: DeleteAccountRequest): Promise<void> {
    await api.delete('/users/me', { data });

    // Log out the user after account deletion
    useAuthStore.getState().logout();
  },

  /**
   * Update the user's profile (firstName, lastName, description).
   * @param data The profile update data
   * @returns The updated user
   */
  async updateProfile(data: {
    firstName: string;
    lastName: string;
    description?: string;
  }): Promise<User> {
    const response = await api.put<User>('/users/me', data);

    // Update the auth store with the new user data
    const token = useAuthStore.getState().token;
    if (token) {
      useAuthStore.getState().setAuth(token, response.data);
    }

    return response.data;
  },

  /**
   * Change the user's password.
   * @param data The password change data
   */
  async changePassword(data: {
    currentPassword: string;
    newPassword: string;
  }): Promise<void> {
    await api.put('/users/me/password', data);
  },
};
