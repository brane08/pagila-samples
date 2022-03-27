import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
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

  getFilmsView(): Observable<PageResult<FilmView>> {
    return this.httpClient.get<PageResult<FilmView>>(`${Constants.API_BASE}/films`);
  }
}
