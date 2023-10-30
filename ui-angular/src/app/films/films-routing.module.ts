import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {FilmsComponent} from "./films.component";
import {FilmListComponent} from "./list/list.component";

const routes: Routes = [
  {
    path: "", component: FilmsComponent, children: [
      {path: "", component: FilmListComponent}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FilmsRoutingModule {

}
