export interface ChatMessage {
  id: string;
  teamId: string;
  userId: string;
  authorName: string;
  authorProfilePhotoUrl?: string | null;
  content: string;
  deleted: boolean;
  edited: boolean;
  createdAt: string;
  editedAt?: string | null;
}

export interface ChatMessagePage {
  content: ChatMessage[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CreateMessageRequest {
  content: string;
}

export interface UpdateMessageRequest {
  content: string;
}
