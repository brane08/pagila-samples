import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FilmsService } from './films.service';
import { Constants } from '../common';

describe('FilmsService', () => {
  let service: FilmsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(FilmsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getFilmsView sends 1-indexed page and size', () => {
    service.getFilmsView(0, 10).subscribe();
    const req = http.expectOne(r => r.url === `${Constants.API_BASE}/films/@view`);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ success: true, data: [], totalCount: 0 });
  });

  it('getNicerFilmsView hits @nicer-view with 1-indexed page', () => {
    service.getNicerFilmsView(2, 5).subscribe();
    const req = http.expectOne(r => r.url === `${Constants.API_BASE}/films/@nicer-view`);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ success: true, data: [], totalCount: 0 });
  });

  it('getSalesByCategory returns the ApiResult as-is', () => {
    let result: unknown;
    service.getSalesByCategory().subscribe(r => result = r);
    const body = { success: true, data: [{ category: 'Action', totalSales: 1 }] };
    http.expectOne(`${Constants.API_BASE}/films/@sales-by-category`).flush(body);
    expect(result).toEqual(body);
  });

  it('getFilmById calls /films/:id', () => {
    service.getFilmById(3).subscribe();
    const req = http.expectOne(`${Constants.API_BASE}/films/3`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('getFilmActors unwraps data', () => {
    let actors: unknown;
    service.getFilmActors(3).subscribe(a => actors = a);
    http.expectOne(`${Constants.API_BASE}/films/3/actors`).flush({ success: true, data: [{ actorId: 1 }] });
    expect(actors).toEqual([{ actorId: 1 }]);
  });
});
