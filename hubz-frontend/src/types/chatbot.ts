export type ChatbotIntent =
  | 'CREATE_TASK'
  | 'CREATE_EVENT'
  | 'CREATE_GOAL'
  | 'CREATE_NOTE'
  | 'QUERY_TASKS'
  | 'QUERY_STATS'
  | 'UNKNOWN';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface ChatMessageRequest {
  message: string;
  organizationId?: string;
}

export interface ExtractedEntities {
  title?: string;
  description?: string;
  date?: string;
  time?: string;
  priority?: TaskPriority;
  additionalEntities?: Record<string, string>;
}

export interface QuickAction {
  label: string;
  action: string;
  url?: string;
}

export interface QueryResults {
  totalCount: number;
  items: Record<string, unknown>[];
  summary: string;
}

export interface ChatbotResponse {
  intent: ChatbotIntent;
  entities: ExtractedEntities;
  confirmationText: string;
  actionUrl?: string;
  actionExecuted: boolean;
  errorMessage?: string;
  createdResourceId?: string;
  quickActions: QuickAction[];
  queryResults?: QueryResults;
  /** Whether Ollama LLM was used for processing (vs regex fallback) */
  usedOllama?: boolean;
  /** The Ollama model name used (if Ollama was used) */
  ollamaModel?: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'bot';
  content: string;
  timestamp: Date;
  response?: ChatbotResponse;
}
