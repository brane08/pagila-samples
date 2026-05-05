import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { Constants } from "../common";
import { Observable } from "rxjs";
import { PageResult } from "../models";
import { ActorView } from "../models/actors";

@Injectable({
  providedIn: 'root'
})
export class ActorsService {

  constructor(private httpClient: HttpClient) {
  }

  getActorViews(page: number, size: number): Observable<PageResult<ActorView>> {
    const params = new HttpParams().append("page", page + 1).append("size", size);
    return this.httpClient.get<PageResult<ActorView>>(`${Constants.API_BASE}/actors/@view`, { params });
  }
}
