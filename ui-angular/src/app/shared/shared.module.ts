import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TableComponent} from './table/table.component';
import {CdkTableModule} from "@angular/cdk/table";
import {PaginationModule} from "ngx-bootstrap/pagination";
import {HttpClientModule} from "@angular/common/http";
import {FormsModule} from "@angular/forms";


@NgModule({
  declarations: [
    TableComponent
  ],
  exports: [
    TableComponent
  ],
    imports: [
        CommonModule,
        HttpClientModule,
        CdkTableModule,
        PaginationModule,
        FormsModule
    ]
})
export class SharedModule { }
