import { Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ChatComponent } from './chat/chat.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
})
export class AppComponent {
  title = 'ui-angular';

  constructor(private dialog: MatDialog) {}

  openChat(): void {
    this.dialog.open(ChatComponent, {
      width: '90vw',
      maxWidth: '90vw',
      height: '85vh',
      panelClass: 'chat-dialog-panel',
      disableClose: true,
      ariaLabel: 'AI Assistant Chat',
    });
  }
}
