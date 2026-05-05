import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { StoresService } from "../../shared/services/stores.service";
import { SalesByStore } from "../../shared/models/stores";

@Component({
  selector: 'app-store-sales',
  templateUrl: './sales.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class StoreSalesComponent {
  private stores = inject(StoresService);

  displayedColumns = ['store', 'manager', 'totalSales'];

  salesResource = rxResource<SalesByStore[], void>({
    stream: () => this.stores.getSalesByStore().pipe(map(r => r.data))
  });
}
