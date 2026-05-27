import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomersRoutingModule } from "./customers-routing.module";
import { CustomersComponent } from "./customers.component";
import { CustomerListComponent } from "./list/list.component";
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatTabsModule } from "@angular/material/tabs";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { RouterModule } from "@angular/router";

@NgModule({
  declarations: [
    CustomersComponent,
    CustomerListComponent
  ],
  imports: [
    CommonModule,
    CustomersRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatTabsModule,
    MatProgressBarModule,
    RouterModule
  ]
})
export class CustomersModule {
}
