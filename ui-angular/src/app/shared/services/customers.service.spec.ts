import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CustomersService } from './customers.service';
import { Constants } from '../common';

describe('CustomersService', () => {
  let service: CustomersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(CustomersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getCustomerViews sends 1-indexed page and size', () => {
    service.getCustomerViews(1, 20).subscribe();
    const req = http.expectOne(r => r.url === `${Constants.API_BASE}/rentals/@customers`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ success: true, data: [], totalCount: 0 });
  });

  it('getCustomerById unwraps data', () => {
    service.getCustomerById(7).subscribe(c => expect(c.id).toBe(7));
    const req = http.expectOne(`${Constants.API_BASE}/rentals/@customers/7`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { id: 7 } });
  });
});
