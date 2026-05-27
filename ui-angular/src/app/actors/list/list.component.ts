import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, tap } from 'rxjs';
import { PageEvent } from "@angular/material/paginator";
import { ActorsService } from "../../shared/services/actors.service";
import { ActorView } from "../../shared/models/actors";
import { defaultPages } from "../../shared/common";

@Component({
  selector: 'app-actor-list',
  templateUrl: './list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ActorListComponent {
  private actors = inject(ActorsService);

  pageNums = defaultPages();
  displayedColumns = ['actorId', 'firstName', 'lastName', 'filmInfo'];
  page = signal(0);
  size = signal(10);
  total = signal(0);

  actorsResource = rxResource<ActorView[], { page: number; size: number }>({
    params: () => ({ page: this.page(), size: this.size() }),
    stream: ({ params }) => this.actors.getActorViews(params.page, params.size).pipe(
      tap(r => this.total.set(r.totalCount)),
      map(r => r.data)
    )
  });

  onPageChange(evt: PageEvent): void {
    this.page.set(evt.pageIndex);
    this.size.set(evt.pageSize);
  }
}
