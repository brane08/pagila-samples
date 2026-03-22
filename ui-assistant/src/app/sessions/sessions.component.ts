import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';

import { AgentService } from '../agent.service';
import { ChatService } from '../chat.service';
import { SessionInfo } from '../chat.models';

@Component({
  selector: 'app-sessions',
  standalone: false,
  templateUrl: './sessions.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionsComponent implements OnInit {
  protected chatService = inject(ChatService);
  private agentService = inject(AgentService);

  sessions = signal<SessionInfo[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.agentService.listSessions().subscribe({
      next: resp => {
        this.sessions.set(resp.sessions);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Could not load sessions');
        this.isLoading.set(false);
      },
    });
  }

  switchSession(threadId: string): void {
    if (this.chatService.currentThreadId() === threadId) return;
    this.chatService.loadSession(threadId);
  }

  deleteSession(threadId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.agentService.deleteSession(threadId).subscribe({
      next: () => {
        this.sessions.update(s => s.filter(s => s.thread_id !== threadId));
        if (this.chatService.currentThreadId() === threadId) {
          this.chatService.newSession();
        }
      },
    });
  }

  newSession(): void {
    this.chatService.newSession();
    // Refresh list after a short delay so the new session has had time to appear
    setTimeout(() => this.refresh(), 300);
  }

  formatDate(iso: string | null): string {
    if (!iso) return '';
    return new Date(iso).toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /** Shorten a long thread_id for display */
  shortId(threadId: string): string {
    return threadId.length > 18 ? '…' + threadId.slice(-15) : threadId;
  }
}
