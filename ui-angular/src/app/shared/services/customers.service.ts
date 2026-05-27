import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { Constants } from "../common";
import { Observable } from "rxjs";
import { PageResult } from "../models";
import { CustomerView } from "../models/customers";

@Injectable({
  providedIn: 'root'
})
export class CustomersService {

  constructor(private httpClient: HttpClient) {
  }

  getCustomerViews(page: number, size: number): Observable<PageResult<CustomerView>> {
    const params = new HttpParams().append("page", page + 1).append("size", size);
    return this.httpClient.get<PageResult<CustomerView>>(`${Constants.API_BASE}/rentals/@customers`, { params });
  }
}
