import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, tap } from 'rxjs';
import { PageEvent } from "@angular/material/paginator";
import { FilmsService } from "../../shared/services";
import { FilmView } from "../../shared/models/films";
import { defaultPages } from "../../shared/common";

@Component({
  selector: 'app-film-list',
  templateUrl: './list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class FilmListComponent {
  private films = inject(FilmsService);

  pageNums = defaultPages();
  displayedColumns = ['filmId', 'name', 'description', 'category'];
  page = signal(0);
  size = signal(10);
  total = signal(0);

  filmsResource = rxResource<FilmView[], { page: number; size: number }>({
    params: () => ({ page: this.page(), size: this.size() }),
    stream: ({ params }) => this.films.getFilmsView(params.page, params.size).pipe(
      tap(r => this.total.set(r.totalCount)),
      map(r => r.data)
    )
  });

  onPageChange(evt: PageEvent): void {
    this.page.set(evt.pageIndex);
    this.size.set(evt.pageSize);
  }
}
