import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
})
export class AppComponent {
  title = 'ui-angular';

  openChat(): void {
    window.open('http://localhost:8000/ui', '_blank');
  }
}
