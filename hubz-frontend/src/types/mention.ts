export interface MentionableUser {
  userId: string;
  firstName: string;
  lastName: string;
  displayName: string;
  mentionName: string;
  profilePhotoUrl: string | null;
}
