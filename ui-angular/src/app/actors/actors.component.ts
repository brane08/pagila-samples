import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-actors',
  templateUrl: './actors.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ActorsComponent {
}
