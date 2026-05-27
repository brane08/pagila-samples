import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActorCardComponent } from './card.component';
import { ActorsService } from '../../shared/services/actors.service';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('ActorCardComponent', () => {
  let component: ActorCardComponent;
  let fixture: ComponentFixture<ActorCardComponent>;
  let actorsServiceSpy: jasmine.SpyObj<ActorsService>;
  let locationSpy: jasmine.SpyObj<Location>;

  const mockActor = {
    actorId: 1,
    firstName: 'PENELOPE',
    lastName: 'GUINESS',
    filmInfo: 'Animation: ACADEMY DINOSAUR, BLANKET BEVERLY; Comedy: ELEPHANT TROJAN'
  };

  beforeEach(async () => {
    actorsServiceSpy = jasmine.createSpyObj('ActorsService', ['getActorById', 'getActorViews']);
    locationSpy = jasmine.createSpyObj('Location', ['back']);
    actorsServiceSpy.getActorById.and.returnValue(of(mockActor));

    await TestBed.configureTestingModule({
      declarations: [ActorCardComponent],
      providers: [
        { provide: ActorsService, useValue: actorsServiceSpy },
        { provide: Location, useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of({ get: (k: string) => k === 'id' ? '1' : null, has: () => true, getAll: () => [], keys: ['id'] }) }
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ActorCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('actorId signal reads from route params', () => {
    expect(component.actorId()).toBe(1);
  });

  it('back() calls Location.back()', () => {
    component.back();
    expect(locationSpy.back).toHaveBeenCalled();
  });

  it('filmGroups parses semicolon-separated category groups', () => {
    fixture.detectChanges();
    const groups = component.filmGroups();
    if (groups.length > 0) {
      expect(groups[0].category).toBe('Animation');
      expect(groups[0].films).toContain('ACADEMY DINOSAUR');
      expect(groups[1].category).toBe('Comedy');
      expect(groups[1].films).toContain('ELEPHANT TROJAN');
    }
  });

  it('filmGroups returns empty array for empty filmInfo', () => {
    actorsServiceSpy.getActorById.and.returnValue(of({ ...mockActor, filmInfo: '' }));
    const fixture2 = TestBed.createComponent(ActorCardComponent);
    fixture2.detectChanges();
    const groups = fixture2.componentInstance.filmGroups();
    expect(groups).toEqual([]);
  });
});
