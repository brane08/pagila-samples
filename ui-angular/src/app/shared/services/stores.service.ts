import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Constants } from "../common";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { ApiResult } from "../models";
import { SalesByStore, StoreDetail } from "../models/stores";
import { StaffView } from "../models/customers";

@Injectable({
  providedIn: 'root'
})
export class StoresService {

  constructor(private httpClient: HttpClient) {
  }

  getSalesByStore(): Observable<ApiResult<SalesByStore[]>> {
    return this.httpClient.get<ApiResult<SalesByStore[]>>(`${Constants.API_BASE}/stores/@sales-by-store`);
  }

  getStaffViews(): Observable<ApiResult<StaffView[]>> {
    return this.httpClient.get<ApiResult<StaffView[]>>(`${Constants.API_BASE}/stores/@staff`);
  }

  getStoreById(id: number): Observable<StoreDetail> {
    return this.httpClient.get<ApiResult<StoreDetail>>(`${Constants.API_BASE}/stores/${id}`)
      .pipe(map(r => r.data));
  }
}
