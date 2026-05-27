import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoresRoutingModule } from "./stores-routing.module";
import { StoresComponent } from "./stores.component";
import { StoreSalesComponent } from "./sales/sales.component";
import { StoreStaffComponent } from "./staff/staff.component";
import { StoreCardComponent } from "./card/card.component";
import { MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatDividerModule } from "@angular/material/divider";
import { MatButtonModule } from "@angular/material/button";
import { RouterModule } from "@angular/router";

@NgModule({
  declarations: [
    StoresComponent,
    StoreSalesComponent,
    StoreStaffComponent,
    StoreCardComponent
  ],
  imports: [
    CommonModule,
    StoresRoutingModule,
    MatTableModule,
    MatTabsModule,
    MatIconModule,
    MatProgressBarModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    RouterModule
  ]
})
export class StoresModule {
}
