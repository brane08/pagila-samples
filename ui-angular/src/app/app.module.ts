import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { FilmsComponent } from './films/films.component';
import {SharedModule} from "./shared/shared.module";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {PaginationModule} from "ngx-bootstrap/pagination";

@NgModule({
  declarations: [
    AppComponent,
    FilmsComponent
  ],
    imports: [
        BrowserModule,
        AppRoutingModule,
        SharedModule,
        BrowserAnimationsModule,
        PaginationModule
    ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
