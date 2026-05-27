import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FilmsComponent } from "./films.component";
import { FilmListComponent } from "./list/list.component";
import { FilmSalesComponent } from "./sales/sales.component";
import { FilmCardComponent } from "./card/card.component";

const routes: Routes = [
  {
    path: "", component: FilmsComponent, children: [
      { path: "", component: FilmListComponent },
      { path: "sales", component: FilmSalesComponent },
      { path: ":id", component: FilmCardComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FilmsRoutingModule {

}
