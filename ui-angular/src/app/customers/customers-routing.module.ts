import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CustomersComponent } from "./customers.component";
import { CustomerListComponent } from "./list/list.component";
import { CustomerCardComponent } from "./card/card.component";

const routes: Routes = [
  {
    path: "", component: CustomersComponent, children: [
      { path: "", component: CustomerListComponent },
      { path: ":id", component: CustomerCardComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CustomersRoutingModule {
}
