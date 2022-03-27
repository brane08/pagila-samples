import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TableDataSource} from "./table-data-source";
import {ColDef, defaultPage, Page} from "../models";
import {Subscription} from "rxjs";
import {PageChangedEvent} from "ngx-bootstrap/pagination";

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: []
})
export class TableComponent implements OnInit, OnDestroy {
  @Input()
  dataSource!: TableDataSource;
  @Input()
  displayColumns!: ColDef[];
  @Input()
  pageNumbers!: number[];

  page: Page = defaultPage();
  columns!: string[];
  total: number = 0;
  totalSub$!: Subscription

  constructor() {
  }

  ngOnInit(): void {
    this.columns = this.displayColumns.map((e) => e.name);
    this.totalSub$ = this.dataSource.total$.subscribe(next => {
      this.total = next;
    });
    this.page.size = this.pageNumbers[0];
  }

  ngOnDestroy(): void {
    this.totalSub$.unsubscribe();
  }

  onPageSizeChange($event: Event) {
    this.dataSource.patchPage(this.page)
  }

  onPageChange($event: PageChangedEvent) {
    this.page.num = $event.page;
    console.log("Page", this.page.num)
    this.dataSource.patchPage(this.page)
  }
}
