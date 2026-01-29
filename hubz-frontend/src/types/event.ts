export interface Event {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
  organizationId?: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
}

export interface UpdateEventRequest {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
}
