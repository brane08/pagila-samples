import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';

import { AgentService } from './agent.service';
import { ChatMessage, MessageRecord, SessionHistory, SseEvent } from './chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private agentService = inject(AgentService);
  private _streamSub: Subscription | null = null;

  private _messages = signal<ChatMessage[]>([]);
  private _currentThreadId = signal<string>(this._newThreadId());

  readonly messages = this._messages.asReadonly();
  readonly currentThreadId = this._currentThreadId.asReadonly();
  readonly isLoading = signal(false);
  readonly messageCount = computed(() => this._messages().length);

  constructor() {
    this._messages.set([this._greeting()]);
  }

  // ── Send a message (starts SSE stream) ──────────────────────────────────────

  sendMessage(text: string): void {
    if (this.isLoading()) return;

    this._streamSub?.unsubscribe();

    const userMsg: ChatMessage = { text, sender: 'user', name: 'You', timestamp: new Date() };
    // Placeholder AI bubble — tokens will be appended into it
    const aiMsg: ChatMessage = {
      text: '',
      sender: 'ai',
      name: 'Assistant',
      timestamp: new Date(),
      isStreaming: true,
    };

    this._messages.update(msgs => [...msgs, userMsg, aiMsg]);
    this.isLoading.set(true);

    this._streamSub = this.agentService
      .streamChat(text, this._currentThreadId())
      .subscribe({
        next: (event: SseEvent) => this._handleSseEvent(event),
        error: () => {
          this._finalizeStreaming('Sorry, I could not reach the assistant. Please try again.');
          this.isLoading.set(false);
        },
      });
  }

  // ── Load an existing session from the backend ────────────────────────────────

  loadSession(threadId: string): void {
    this._streamSub?.unsubscribe();
    this._currentThreadId.set(threadId);
    this._messages.set([]);
    this.isLoading.set(true);

    this.agentService.getSession(threadId).subscribe({
      next: (history: SessionHistory) => {
        const msgs = history.messages
          .map(m => this._recordToMessage(m))
          .filter((m): m is ChatMessage => m !== null);
        this._messages.set(msgs.length ? msgs : [this._greeting()]);
        this.isLoading.set(false);
      },
      error: () => {
        this._messages.set([this._greeting()]);
        this.isLoading.set(false);
      },
    });
  }

  // ── Start a fresh session ────────────────────────────────────────────────────

  newSession(): void {
    this._streamSub?.unsubscribe();
    this._currentThreadId.set(this._newThreadId());
    this._messages.set([this._greeting()]);
    this.isLoading.set(false);
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private _handleSseEvent(event: SseEvent): void {
    switch (event.type) {
      case 'token':
        // Append token to the last (streaming) AI message
        this._messages.update(msgs => {
          const last = { ...msgs[msgs.length - 1], text: msgs[msgs.length - 1].text + (event.content ?? '') };
          return [...msgs.slice(0, -1), last];
        });
        break;

      case 'tool_start':
        // Insert a tool-status row before the still-empty AI bubble
        const toolMsg: ChatMessage = {
          text: `🔧 Using **${event.tool}**…`,
          sender: 'tool',
          name: 'System',
          timestamp: new Date(),
        };
        this._messages.update(msgs => {
          const last = msgs[msgs.length - 1];
          return [...msgs.slice(0, -1), toolMsg, last];
        });
        break;

      case 'done':
        this._messages.update(msgs => {
          const last = { ...msgs[msgs.length - 1], isStreaming: false };
          return [...msgs.slice(0, -1), last];
        });
        this.isLoading.set(false);
        break;
    }
  }

  private _finalizeStreaming(fallbackText: string): void {
    this._messages.update(msgs => {
      const last = msgs[msgs.length - 1];
      return [
        ...msgs.slice(0, -1),
        { ...last, text: last.text || fallbackText, isStreaming: false },
      ];
    });
  }

  /** Map an API MessageRecord to a local ChatMessage (returns null for skippable records). */
  private _recordToMessage(record: MessageRecord): ChatMessage | null {
    // Blank AI messages that only carry tool-call metadata are internal LangGraph artefacts
    if (record.role === 'assistant' && !record.content && record.tool_calls.length > 0) {
      return null;
    }
    if (record.role === 'tool') {
      return {
        text: `🔧 **${record.tool_name ?? 'Tool'}** executed`,
        sender: 'tool',
        name: 'System',
        timestamp: new Date(),
      };
    }
    return {
      text: record.content,
      sender: record.role === 'user' ? 'user' : 'ai',
      name: record.role === 'user' ? 'You' : 'Assistant',
      timestamp: new Date(),
    };
  }

  private _greeting(): ChatMessage {
    return {
      text: 'Hi! Ask me anything about films, actors, or rentals.',
      sender: 'ai',
      name: 'Assistant',
      timestamp: new Date(),
    };
  }

  private _newThreadId(): string {
    return `session-${Date.now()}`;
  }
}
