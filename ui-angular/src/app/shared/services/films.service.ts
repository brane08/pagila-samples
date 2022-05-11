import {Injectable} from '@angular/core';
import {QueryApiService} from "./query-api.service";
import {environment} from "../../../environments/environment";
import {TableDataSource} from "../table/table-data-source";
import {ColDef} from "../models";
import {HttpClient} from "@angular/common/http";

const API_URL = environment.apiUrl;

@Injectable({
  providedIn: 'root'
})
export class FilmsService {

  constructor(private http: HttpClient, private queryAPI: QueryApiService) {
  }

  dataSource(): TableDataSource {
    return new TableDataSource("/films", this.queryAPI);
  }

  colDefs(): ColDef[] {
    return [
      {type: "select", header: "#", name: "selected", sort: undefined},
      {type: "number", header: "ID", name: "filmId", sort: undefined},
      {type: "string", header: "Name", name: "title", sort: undefined},
      {type: "number", header: "Release Year", name: "releaseYear", sort: undefined},
      {type: "language", header: "Language", name: "language", sort: undefined},
      {type: "string", header: "Description", name: "description", sort: undefined},
    ];
  }

}
