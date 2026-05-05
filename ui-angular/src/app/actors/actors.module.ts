import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActorsRoutingModule } from "./actors-routing.module";
import { ActorsComponent } from "./actors.component";
import { ActorListComponent } from "./list/list.component";
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatTabsModule } from "@angular/material/tabs";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { RouterModule } from "@angular/router";

@NgModule({
  declarations: [
    ActorsComponent,
    ActorListComponent
  ],
  imports: [
    CommonModule,
    ActorsRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatTabsModule,
    MatProgressBarModule,
    RouterModule
  ]
})
export class ActorsModule {
}
