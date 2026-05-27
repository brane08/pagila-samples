import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActorsRoutingModule } from "./actors-routing.module";
import { ActorsComponent } from "./actors.component";
import { ActorListComponent } from "./list/list.component";
import { ActorCardComponent } from "./card/card.component";
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatTabsModule } from "@angular/material/tabs";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatDividerModule } from "@angular/material/divider";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { RouterModule } from "@angular/router";

@NgModule({
  declarations: [
    ActorsComponent,
    ActorListComponent,
    ActorCardComponent
  ],
  imports: [
    CommonModule,
    ActorsRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatTabsModule,
    MatProgressBarModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    RouterModule
  ]
})
export class ActorsModule {
}
