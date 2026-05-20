import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { Constants } from "../common";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { ApiResult, PageResult } from "../models";
import { ActorDetail, ActorView } from "../models/actors";

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

  getActorById(id: number): Observable<ActorDetail> {
    return this.httpClient.get<ApiResult<ActorDetail>>(`${Constants.API_BASE}/actors/${id}`)
      .pipe(map(r => r.data));
  }
}
