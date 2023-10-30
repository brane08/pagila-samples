import {Component, OnDestroy, OnInit} from '@angular/core';
import {FilmsService} from "../../shared/services";
import {Subscription} from "rxjs";
import {MatTableDataSource} from "@angular/material/table";
import {PageEvent} from "@angular/material/paginator";
import {defaultPages} from "../../shared/common";

@Component({
  selector: 'app-film-list',
  templateUrl: './list.component.html',
  styleUrls: []
})
export class FilmListComponent implements OnInit, OnDestroy {

  pageNums = defaultPages();
  dataSource = new MatTableDataSource();
  displayedColumns: string[] = ['filmId', 'name', 'description', 'category'];
  page = 0;
  size = 10;
  total = 0;
  private _subs: Subscription[] = []

  constructor(private films: FilmsService) {
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
    for (let sub of this._subs) {
      if (sub) {
        sub.unsubscribe();
      }
    }
  }

  ngOnInit(): void {
    this.fetchPage();
    this.dataSource.connect();
  }

  fetchPage() {
    this._subs.push(this.films.getFilmsView(this.page, this.size).subscribe(response => {
      this.dataSource.data = response.data;
      this.total = response.totalCount;
    }));
  }

  onPageChange(evt: PageEvent) {
    this.page = evt.pageIndex;
    this.size = evt.pageSize;
    this.total = evt.length;
    console.log(`Page: ${this.page}, Size: ${this.size}`);
    this.fetchPage();
  }
}
