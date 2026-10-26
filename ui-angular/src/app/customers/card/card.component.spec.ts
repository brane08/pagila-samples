import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { CustomerCardComponent } from './card.component';
import { CustomersService } from '../../shared/services/customers.service';

describe('CustomerCardComponent', () => {
  let fixture: ComponentFixture<CustomerCardComponent>;
  let component: CustomerCardComponent;
  let service: jasmine.SpyObj<CustomersService>;
  let location: jasmine.SpyObj<Location>;

  const customer = {
    id: 7, name: 'MARY SMITH', address: '1913 Hanoi Way', zipCode: '35200',
    phone: '28303384290', city: 'Sasebo', country: 'Japan', notes: 'active', sid: 1
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('CustomersService', ['getCustomerById']);
    location = jasmine.createSpyObj('Location', ['back']);
    service.getCustomerById.and.returnValue(of(customer));

    await TestBed.configureTestingModule({
      declarations: [CustomerCardComponent],
      providers: [
        { provide: CustomersService, useValue: service },
        { provide: Location, useValue: location },
        { provide: ActivatedRoute, useValue: { paramMap: of({ get: (k: string) => k === 'id' ? '7' : null }) } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('reads customerId from route params', () => {
    expect(component.customerId()).toBe(7);
  });

  it('loads the customer by id', async () => {
    await fixture.whenStable();
    expect(service.getCustomerById).toHaveBeenCalledWith(7);
    expect(component.customerResource.value()).toEqual(customer);
  });

  it('renders name and details', async () => {
    await fixture.whenStable();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('MARY SMITH');
    expect(text).toContain('Sasebo');
  });

  it('back() calls Location.back()', () => {
    component.back();
    expect(location.back).toHaveBeenCalled();
  });

  it('shows not-found state when the lookup fails', async () => {
    service.getCustomerById.and.returnValue(throwError(() => new Error('404')));
    const f = TestBed.createComponent(CustomerCardComponent);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).toContain('Customer not found.');
  });
});
