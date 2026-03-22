import { Component, signal } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ChatComponent } from './chat/chat.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('ui-assistant');

  constructor(private modalService: NgbModal) {
  }

  openChat() {
    this.modalService.open(ChatComponent, {
      size: 'xl',
      backdrop: 'static',
      keyboard: false,
      windowClass: 'chat-modal',
    });
  }
}
