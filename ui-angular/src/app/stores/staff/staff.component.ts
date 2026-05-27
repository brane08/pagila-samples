import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { StoresService } from "../../shared/services/stores.service";
import { StaffView } from "../../shared/models/customers";

@Component({
  selector: 'app-store-staff',
  templateUrl: './staff.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class StoreStaffComponent {
  private stores = inject(StoresService);

  displayedColumns = ['id', 'name', 'city', 'country', 'phone'];

  staffResource = rxResource<StaffView[], void>({
    stream: () => this.stores.getStaffViews().pipe(map(r => r.data))
  });
}
