import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from "@angular/common/http";
import {Constants} from "../common";
import {Observable} from "rxjs";
import {PageResult} from "../models";
import {FilmView} from "../models/films";

@Injectable({
  providedIn: 'root'
})
export class FilmsService {

  constructor(private httpClient: HttpClient) {
  }

  getFilmsView(page: number, size: number): Observable<PageResult<FilmView>> {
    let params = new HttpParams().append("page", (page + 1)).append("size", size);
    return this.httpClient.get<PageResult<FilmView>>(`${Constants.API_BASE}/films/@view`, {params});
  }
}
