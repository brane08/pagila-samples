import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { FilmsService } from '../../shared/services';
import { ActorInfo, FilmDetail } from '../../shared/models/films';

@Component({
  selector: 'app-film-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class FilmCardComponent {
  private route = inject(ActivatedRoute);
  private filmsService = inject(FilmsService);
  private location = inject(Location);

  filmId = toSignal(
    this.route.paramMap.pipe(map(p => Number(p.get('id')))),
    { initialValue: 0 }
  );

  filmResource = rxResource<FilmDetail, number>({
    params: () => this.filmId(),
    stream: ({ params }) => params > 0 ? this.filmsService.getFilmById(params) : EMPTY
  });

  actorsResource = rxResource<ActorInfo[], number>({
    params: () => this.filmId(),
    stream: ({ params }) => params > 0 ? this.filmsService.getFilmActors(params) : EMPTY
  });

  back(): void {
    this.location.back();
  }
}
