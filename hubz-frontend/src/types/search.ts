export interface SearchResultResponse {
  organizations: OrganizationSearchResult[];
  tasks: TaskSearchResult[];
  goals: GoalSearchResult[];
  events: EventSearchResult[];
  notes: NoteSearchResult[];
  members: MemberSearchResult[];
  totalResults: number;
}

export interface OrganizationSearchResult {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  color: string | null;
  matchedField: string;
}

export interface TaskSearchResult {
  id: string;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  organizationId: string;
  organizationName: string;
  matchedField: string;
}

export interface GoalSearchResult {
  id: string;
  title: string;
  description: string | null;
  type: string;
  deadline: string | null;
  organizationId: string | null;
  organizationName: string;
  matchedField: string;
}

export interface EventSearchResult {
  id: string;
  title: string;
  description: string | null;
  startTime: string | null;
  endTime: string | null;
  organizationId: string | null;
  organizationName: string;
  matchedField: string;
}

export interface NoteSearchResult {
  id: string;
  title: string;
  content: string | null;
  category: string | null;
  organizationId: string;
  organizationName: string;
  matchedField: string;
}

export interface MemberSearchResult {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  organizationId: string;
  organizationName: string;
  role: string;
  matchedField: string;
}
