import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActorsService } from './actors.service';
import { Constants } from '../common';

describe('ActorsService', () => {
  let service: ActorsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ActorsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getActorViews sends page+size as 1-indexed', () => {
    service.getActorViews(0, 10).subscribe();
    const req = http.expectOne(r => r.url.includes('/actors/@view'));
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ success: true, data: [], totalCount: 0 });
  });

  it('getActorViews converts 0-indexed page to 1-indexed', () => {
    service.getActorViews(2, 25).subscribe();
    const req = http.expectOne(r => r.url.includes('/actors/@view'));
    expect(req.request.params.get('page')).toBe('3');
    req.flush({ success: true, data: [], totalCount: 0 });
  });

  it('getActorById calls /actors/:id and unwraps data', () => {
    const mockActor = { actorId: 5, firstName: 'PENELOPE', lastName: 'GUINESS', filmInfo: 'Action: FILM1' };
    service.getActorById(5).subscribe(actor => {
      expect(actor.actorId).toBe(5);
      expect(actor.firstName).toBe('PENELOPE');
    });
    const req = http.expectOne(`${Constants.API_BASE}/actors/5`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockActor });
  });

  it('getActorById sends a GET request with the correct URL', () => {
    service.getActorById(42).subscribe();
    const req = http.expectOne(`${Constants.API_BASE}/actors/42`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { actorId: 42, firstName: 'ED', lastName: 'CHASE', filmInfo: '' } });
  });
});
