import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FilmsRoutingModule} from "./films-routing.module";
import {FilmListComponent} from './list/list.component';
import {FilmsComponent} from './films.component';
import {MatTableModule} from "@angular/material/table";
import {MatPaginatorModule} from "@angular/material/paginator";
import {FilmCardComponent} from './card/card.component';
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatIconModule} from "@angular/material/icon";


@NgModule({
  declarations: [
    FilmsComponent,
    FilmListComponent,
    FilmCardComponent
  ],
  imports: [
    CommonModule,
    FilmsRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonToggleModule,
    MatIconModule
  ]
})
export class FilmsModule {
}
