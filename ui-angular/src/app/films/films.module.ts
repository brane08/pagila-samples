import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FilmsRoutingModule } from "./films-routing.module";
import { FilmListComponent } from './list/list.component';
import { FilmsComponent } from './films.component';
import { FilmSalesComponent } from './sales/sales.component';
import { FilmCardComponent } from './card/card.component';
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatDividerModule } from "@angular/material/divider";
import { MatButtonModule } from "@angular/material/button";
import { RouterModule } from "@angular/router";


@NgModule({
  declarations: [
    FilmsComponent,
    FilmListComponent,
    FilmSalesComponent,
    FilmCardComponent
  ],
  imports: [
    CommonModule,
    FilmsRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonToggleModule,
    MatIconModule,
    MatTabsModule,
    MatProgressBarModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    RouterModule
  ]
})
export class FilmsModule {
}
