import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {BehaviorSubject, finalize, Observable} from "rxjs";
import {QueryApiService} from "../services";
import {BaseType, defaultPageRequest, Page, PageRequest} from "../models";

export class TableDataSource extends DataSource<object> {

  private _url = "";
  private _apiService: QueryApiService;
  private dataSubject = new BehaviorSubject<object[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private totalSubject = new BehaviorSubject<number>(0);

  pageRequest: PageRequest;
  readonly loading$ = this.loadingSubject.asObservable();
  readonly total$ = this.totalSubject.asObservable();

  constructor(url: string, apiService: QueryApiService) {
    super();
    this._url = url;
    this._apiService = apiService;
    this.pageRequest = defaultPageRequest();
  }

  connect(collectionViewer: CollectionViewer): Observable<object[]> {
    return this.dataSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.dataSubject.complete();
    this.loadingSubject.complete();
  }

  private loadData(page: Page, filter?: Record<string, BaseType>) {
    this.pageRequest = {current: page, filters: filter};
    this.loadingSubject.next(true);
    this._apiService.query(this._url, this.pageRequest).pipe(
      finalize(() => this.loadingSubject.next(false))
    ).subscribe(response => {
      console.log(response.data);
      this.dataSubject.next(response.data);
    });
    this._apiService.counts(this._url, this.pageRequest).subscribe(response => {
      this.totalSubject.next(response.data["total"]);
    });
  }

  patchFilters(filters: Record<string, BaseType>) {
    this.loadData(this.pageRequest.current, filters);
  }

  patchPage(page: Page) {
    this.loadData(page, this.pageRequest.filters);
  }

  patch(page: Page, filters: Record<string, BaseType>) {
    this.loadData(page, filters);
  }
}
