import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StoresService } from './stores.service';
import { Constants } from '../common';

describe('StoresService', () => {
  let service: StoresService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(StoresService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSalesByStore calls @sales-by-store', () => {
    service.getSalesByStore().subscribe();
    const req = http.expectOne(`${Constants.API_BASE}/stores/@sales-by-store`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('getStaffViews calls @staff', () => {
    service.getStaffViews().subscribe();
    const req = http.expectOne(`${Constants.API_BASE}/stores/@staff`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('getStoreById calls /stores/:id and unwraps data', () => {
    const mockStore = {
      storeId: 1,
      manager: { staffId: 1, firstName: 'Mike', lastName: 'Hillyer', email: 'm@s.com', username: 'Mike' },
      address: { address: '23 Lane', address2: null, district: 'Alberta', postalCode: '', phone: '123',
                  city: { city: 'Lethbridge', country: { country: 'Canada' } } },
      currentStaff: []
    };
    service.getStoreById(1).subscribe(store => {
      expect(store.storeId).toBe(1);
      expect(store.manager.firstName).toBe('Mike');
    });
    const req = http.expectOne(`${Constants.API_BASE}/stores/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockStore });
  });

  it('getStoreById sends a GET request with the correct URL', () => {
    service.getStoreById(2).subscribe();
    const req = http.expectOne(`${Constants.API_BASE}/stores/2`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {
      storeId: 2, manager: { staffId: 2, firstName: 'Jon', lastName: 'Stephens', email: 'j@s.com', username: 'Jon' },
      address: { address: '1411 Drive', address2: null, district: 'QLD', postalCode: '', phone: '456',
                  city: { city: 'Woodridge', country: { country: 'Australia' } } },
      currentStaff: []
    }});
  });
});
