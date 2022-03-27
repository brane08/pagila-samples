import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FilmsRoutingModule} from "./films-routing.module";
import {FilmListComponent} from './list/list.component';
import { FilmsComponent } from './films.component';
import {RouterModule} from "@angular/router";


@NgModule({
  declarations: [
    FilmListComponent,
    FilmsComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FilmsRoutingModule
  ]
})
export class FilmsModule {
}
