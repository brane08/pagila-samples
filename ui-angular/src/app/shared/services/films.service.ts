import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { Constants } from "../common";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { ApiResult, PageResult } from "../models";
import { ActorInfo, FilmDetail, FilmView, NicerFilmView, SalesByFilmCategory } from "../models/films";

@Injectable({
  providedIn: 'root'
})
export class FilmsService {

  constructor(private httpClient: HttpClient) {
  }

  getFilmsView(page: number, size: number): Observable<PageResult<FilmView>> {
    let params = new HttpParams().append("page", (page + 1)).append("size", size);
    return this.httpClient.get<PageResult<FilmView>>(`${Constants.API_BASE}/films/@view`, { params });
  }

  getNicerFilmsView(page: number, size: number): Observable<PageResult<NicerFilmView>> {
    let params = new HttpParams().append("page", (page + 1)).append("size", size);
    return this.httpClient.get<PageResult<NicerFilmView>>(`${Constants.API_BASE}/films/@nicer-view`, { params });
  }

  getSalesByCategory(): Observable<ApiResult<SalesByFilmCategory[]>> {
    return this.httpClient.get<ApiResult<SalesByFilmCategory[]>>(`${Constants.API_BASE}/films/@sales-by-category`);
  }

  getFilmById(id: number): Observable<FilmDetail> {
    return this.httpClient.get<FilmDetail>(`${Constants.API_BASE}/films/${id}`);
  }

  getFilmActors(id: number): Observable<ActorInfo[]> {
    return this.httpClient.get<ApiResult<ActorInfo[]>>(`${Constants.API_BASE}/films/${id}/actors`).pipe(
      map(r => r.data)
    );
  }
}
