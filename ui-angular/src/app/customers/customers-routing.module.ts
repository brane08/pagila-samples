import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CustomersComponent } from "./customers.component";
import { CustomerListComponent } from "./list/list.component";

const routes: Routes = [
  {
    path: "", component: CustomersComponent, children: [
      { path: "", component: CustomerListComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CustomersRoutingModule {
}
