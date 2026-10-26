import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Constants } from './shared/common';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  title = 'ui-angular';

  openChat(): void {
    window.open(Constants.ASSISTANT_UI_URL, '_blank', 'noopener');
  }
}
