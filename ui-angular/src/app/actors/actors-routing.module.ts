import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ActorsComponent } from "./actors.component";
import { ActorListComponent } from "./list/list.component";

const routes: Routes = [
  {
    path: "", component: ActorsComponent, children: [
      { path: "", component: ActorListComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ActorsRoutingModule {
}
