import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { CustomersService } from '../../shared/services/customers.service';
import { CustomerView } from '../../shared/models/customers';

@Component({
  selector: 'app-customer-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CustomerCardComponent {
  private route = inject(ActivatedRoute);
  private customersService = inject(CustomersService);
  private location = inject(Location);

  customerId = toSignal(
    this.route.paramMap.pipe(map(p => Number(p.get('id')))),
    { initialValue: 0 }
  );

  customerResource = rxResource<CustomerView, number>({
    params: () => this.customerId(),
    stream: ({ params }) => params > 0 ? this.customersService.getCustomerById(params) : throwError(() => new Error('Invalid customer id'))
  });

  back(): void {
    this.location.back();
  }
}
