import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-customers',
  templateUrl: './customers.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CustomersComponent {
}
