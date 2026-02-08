export type RecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface Event {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
  location?: string;
  reminder?: string;
  organizationId?: string;
  userId: string;
  // Recurrence fields
  recurrenceType?: RecurrenceType;
  recurrenceInterval?: number;
  recurrenceEndDate?: string;
  parentEventId?: string;
  originalDate?: string;
  isRecurrenceException?: boolean;
  isRecurring?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
  location?: string;
  reminder?: string;
  participantIds?: string[];
  // Recurrence fields
  recurrenceType?: RecurrenceType;
  recurrenceInterval?: number;
  recurrenceEndDate?: string;
}

export interface UpdateEventRequest {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
  location?: string;
  reminder?: string;
  // Recurrence fields
  recurrenceType?: RecurrenceType;
  recurrenceInterval?: number;
  recurrenceEndDate?: string;
  updateAllOccurrences?: boolean;
}
