import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FilmsService } from "../../shared/services";
import { SalesByFilmCategory } from "../../shared/models/films";

@Component({
  selector: 'app-film-sales',
  templateUrl: './sales.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class FilmSalesComponent {
  private films = inject(FilmsService);

  displayedColumns = ['category', 'totalSales'];

  salesResource = rxResource<SalesByFilmCategory[], void>({
    stream: () => this.films.getSalesByCategory().pipe(map(r => r.data))
  });
}
