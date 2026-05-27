import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreCardComponent } from './card.component';
import { StoresService } from '../../shared/services/stores.service';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('StoreCardComponent', () => {
  let component: StoreCardComponent;
  let fixture: ComponentFixture<StoreCardComponent>;
  let storesServiceSpy: jasmine.SpyObj<StoresService>;
  let locationSpy: jasmine.SpyObj<Location>;

  const mockStore = {
    storeId: 1,
    manager: { staffId: 1, firstName: 'Mike', lastName: 'Hillyer', email: 'm@s.com', username: 'Mike' },
    address: {
      address: '23 Workhaven Lane',
      address2: null,
      district: 'Alberta',
      postalCode: '',
      phone: '14033335568',
      city: { city: 'Lethbridge', country: { country: 'Canada' } }
    },
    currentStaff: [
      { staffId: 1, firstName: 'Mike', lastName: 'Hillyer', email: 'm@s.com', username: 'Mike' }
    ]
  };

  beforeEach(async () => {
    storesServiceSpy = jasmine.createSpyObj('StoresService', ['getStoreById', 'getSalesByStore', 'getStaffViews']);
    locationSpy = jasmine.createSpyObj('Location', ['back']);
    storesServiceSpy.getStoreById.and.returnValue(of(mockStore));

    await TestBed.configureTestingModule({
      declarations: [StoreCardComponent],
      providers: [
        { provide: StoresService, useValue: storesServiceSpy },
        { provide: Location, useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of({ get: (k: string) => k === 'id' ? '1' : null, has: () => true, getAll: () => [], keys: ['id'] }) }
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('storeId signal reads from route params', () => {
    expect(component.storeId()).toBe(1);
  });

  it('back() calls Location.back()', () => {
    component.back();
    expect(locationSpy.back).toHaveBeenCalled();
  });
});
