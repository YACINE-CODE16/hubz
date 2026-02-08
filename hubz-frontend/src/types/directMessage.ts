export interface DirectMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderProfilePhotoUrl: string | null;
  receiverId: string;
  receiverName: string;
  receiverProfilePhotoUrl: string | null;
  content: string;
  read: boolean;
  deleted: boolean;
  edited: boolean;
  createdAt: string;
  editedAt: string | null;
}

export interface Conversation {
  userId: string;
  userName: string;
  userProfilePhotoUrl: string | null;
  lastMessageContent: string;
  lastMessageSenderId: string;
  lastMessageAt: string;
  unreadCount: number;
}

export interface SendDirectMessageRequest {
  receiverId: string;
  content: string;
}

export interface UpdateDirectMessageRequest {
  content: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
