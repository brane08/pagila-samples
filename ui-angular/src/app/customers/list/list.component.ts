import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, tap } from 'rxjs';
import { PageEvent } from "@angular/material/paginator";
import { CustomersService } from "../../shared/services/customers.service";
import { CustomerView } from "../../shared/models/customers";
import { defaultPages } from "../../shared/common";

@Component({
  selector: 'app-customer-list',
  templateUrl: './list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CustomerListComponent {
  private customers = inject(CustomersService);

  pageNums = defaultPages();
  displayedColumns = ['id', 'name', 'city', 'country', 'phone', 'notes'];
  page = signal(0);
  size = signal(10);
  total = signal(0);

  customersResource = rxResource<CustomerView[], { page: number; size: number }>({
    params: () => ({ page: this.page(), size: this.size() }),
    stream: ({ params }) => this.customers.getCustomerViews(params.page, params.size).pipe(
      tap(r => this.total.set(r.totalCount)),
      map(r => r.data)
    )
  });

  onPageChange(evt: PageEvent): void {
    this.page.set(evt.pageIndex);
    this.size.set(evt.pageSize);
  }
}
