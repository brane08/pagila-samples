import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from "./home/home.component";
import { provideCharts, withDefaultRegisterables, } from 'ng2-charts';

const routes: Routes = [
  { path: "", component: HomeComponent, pathMatch: "full" },
  { path: "films", loadChildren: () => import("./films/films.module").then(m => m.FilmsModule) },
  { path: "actors", loadChildren: () => import("./actors/actors.module").then(m => m.ActorsModule) }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  providers: [
    provideCharts(withDefaultRegisterables()),
  ]
})
export class AppRoutingModule {
}
