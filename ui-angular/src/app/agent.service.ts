import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ChatApiResponse,
  SessionHistory,
  SessionInfo,
  SseEvent,
} from './chat.models';

const BASE_URL = 'http://localhost:8000';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private http = inject(HttpClient);

  chat(message: string, threadId: string): Observable<ChatApiResponse> {
    return this.http.post<ChatApiResponse>(`${BASE_URL}/chat`, {
      message,
      thread_id: threadId,
    });
  }

  // EventSource only supports GET, so we use fetch and parse the SSE protocol manually.
  streamChat(message: string, threadId: string): Observable<SseEvent> {
    return new Observable(observer => {
      const controller = new AbortController();

      fetch(`${BASE_URL}/chat/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, thread_id: threadId }),
        signal: controller.signal,
      })
        .then(async response => {
          if (!response.ok) {
            observer.error(new Error(`HTTP ${response.status}: ${response.statusText}`));
            return;
          }

          const reader = response.body!.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          while (true) {
            const { done, value } = await reader.read();
            if (done) {
              observer.complete();
              break;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop()!;

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6).trim();
                if (data) {
                  try {
                    observer.next(JSON.parse(data) as SseEvent);
                  } catch { /* skip malformed frames */ }
                }
              }
            }
          }
        })
        .catch(err => {
          if (err?.name !== 'AbortError') observer.error(err);
        });

      return () => controller.abort();
    });
  }

  listSessions(): Observable<{ sessions: SessionInfo[]; total: number }> {
    return this.http.get<{ sessions: SessionInfo[]; total: number }>(
      `${BASE_URL}/sessions`
    );
  }

  getSession(threadId: string): Observable<SessionHistory> {
    return this.http.get<SessionHistory>(
      `${BASE_URL}/sessions/${encodeURIComponent(threadId)}`
    );
  }

  deleteSession(threadId: string): Observable<{ deleted: boolean; thread_id: string }> {
    return this.http.delete<{ deleted: boolean; thread_id: string }>(
      `${BASE_URL}/sessions/${encodeURIComponent(threadId)}`
    );
  }
}
