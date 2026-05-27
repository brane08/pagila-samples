import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { StoresService } from '../../shared/services/stores.service';
import { StoreDetail } from '../../shared/models/stores';

@Component({
  selector: 'app-store-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class StoreCardComponent {
  private route = inject(ActivatedRoute);
  private storesService = inject(StoresService);
  private location = inject(Location);

  storeId = toSignal(
    this.route.paramMap.pipe(map(p => Number(p.get('id')))),
    { initialValue: 0 }
  );

  storeResource = rxResource<StoreDetail, number>({
    params: () => this.storeId(),
    stream: ({ params }) => params > 0 ? this.storesService.getStoreById(params) : EMPTY
  });

  back(): void {
    this.location.back();
  }
}
