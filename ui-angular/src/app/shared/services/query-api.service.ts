import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {environment} from "../../../environments/environment";
import {ApiResponse, PageRequest, PageResponse, toHttpParams} from "../models";
import {Observable} from "rxjs";
import {BasicType} from "../models/common.model";

const API_URL = environment.apiUrl

@Injectable({
  providedIn: 'root'
})
export class QueryApiService {

  constructor(private http: HttpClient) {
  }

  query(url: string, request: PageRequest): Observable<PageResponse<Record<string, BasicType>>> {
    return this.http.get<PageResponse<Record<string, BasicType>>>(`${API_URL}${url}`, {params: toHttpParams(request)})
  }

  counts(url: string, request: PageRequest): Observable<ApiResponse<Record<string, number>>> {
    return this.http.get<ApiResponse<Record<string, number>>>(`${API_URL}${url}/counts`, {params: toHttpParams(request)});
  }
}
