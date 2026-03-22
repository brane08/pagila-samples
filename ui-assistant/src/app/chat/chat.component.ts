import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  inject,
  viewChild,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';

import { ChatService } from '../chat.service';

@Component({
  selector: 'app-chat',
  standalone: false,
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatComponent {
  protected chatService = inject(ChatService);
  chatHistory = viewChild<ElementRef>('chatHistory');

  messageForm = new FormControl('', [Validators.required, Validators.minLength(1)]);

  constructor() {
    // Auto-scroll whenever messages change
    effect(() => {
      this.chatService.messages();
      const el = this.chatHistory()?.nativeElement;
      if (el) setTimeout(() => (el.scrollTop = el.scrollHeight), 0);
    });
  }

  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendMessage(): void {
    const text = this.messageForm.value?.trim();
    if (!text || this.chatService.isLoading()) return;
    this.messageForm.reset();
    this.chatService.sendMessage(text);
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  get isSendDisabled(): boolean {
    return !this.messageForm.value?.trim() || this.chatService.isLoading();
  }
}
