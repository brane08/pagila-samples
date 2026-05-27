// ── Local display model ────────────────────────────────────────────────────────

export interface ChatMessage {
  text: string;
  sender: 'user' | 'ai' | 'tool';
  name: string;
  timestamp: Date;
  isStreaming?: boolean; // true while tokens are still arriving
}

export interface ToolInfo {
  name: string;
  description?: string;
  parameters: any;
}

// ── FastAPI response types ─────────────────────────────────────────────────────

export interface ChatApiResponse {
  answer: string;
  tool_calls_made: string[];
}

/** One event from the POST /chat/stream SSE endpoint */
export interface SseEvent {
  type: 'token' | 'tool_start' | 'tool_end' | 'done' | 'tool_confirm';
  content?: string;
  tool?: string;
  input?: any;
  thread_id?: string;
  tool_calls?: Array<{ name: string; args: any; id: string }>;
}

// ── Session types ──────────────────────────────────────────────────────────────

export interface SessionInfo {
  thread_id: string;
  step_count: number;
  last_active: string | null;
}

export interface MessageRecord {
  role: 'user' | 'assistant' | 'tool';
  content: string;
  tool_calls: string[];
  tool_name: string | null;
}

export interface SessionHistory {
  thread_id: string;
  messages: MessageRecord[];
  last_active: string | null;
}
