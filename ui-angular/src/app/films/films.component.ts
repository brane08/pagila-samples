import {Component, OnInit} from '@angular/core';
import {FilmsService} from "../shared/services";
import {ColDef, defaultPage} from "../shared/models";
import {TableDataSource} from "../shared/table/table-data-source";

@Component({
  selector: 'app-films',
  templateUrl: './films.component.html',
  styleUrls: []
})
export class FilmsComponent implements OnInit {

  colDefs: ColDef[];
  dataSource: TableDataSource;
  total: number = 0;
  pages = [10, 25, 50, 100];

  constructor(private filmsService: FilmsService) {
    this.colDefs = filmsService.colDefs();
    this.dataSource = filmsService.dataSource();
  }

  ngOnInit(): void {
    this.dataSource.patchPage(defaultPage());
  }

  showValues($event: MouseEvent) {
    let count = 0;
    for (let dsElem of this.dataSource.currentData()) {
      if (dsElem["selected"]) {
        count++;
      }
    }
    console.log("counts:", count);
  }
}
