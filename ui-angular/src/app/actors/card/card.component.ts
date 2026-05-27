import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { ActorsService } from '../../shared/services/actors.service';
import { ActorDetail } from '../../shared/models/actors';

@Component({
  selector: 'app-actor-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ActorCardComponent {
  private route = inject(ActivatedRoute);
  private actorsService = inject(ActorsService);
  private location = inject(Location);

  actorId = toSignal(
    this.route.paramMap.pipe(map(p => Number(p.get('id')))),
    { initialValue: 0 }
  );

  actorResource = rxResource<ActorDetail, number>({
    params: () => this.actorId(),
    stream: ({ params }) => params > 0 ? this.actorsService.getActorById(params) : EMPTY
  });

  filmGroups = computed(() => {
    const info = this.actorResource.value()?.filmInfo ?? '';
    if (!info) return [];
    return info.split('; ').map(group => {
      const colonIdx = group.indexOf(': ');
      if (colonIdx === -1) return { category: group, films: [] as string[] };
      return {
        category: group.slice(0, colonIdx),
        films: group.slice(colonIdx + 2).split(', ')
      };
    });
  });

  back(): void {
    this.location.back();
  }
}
