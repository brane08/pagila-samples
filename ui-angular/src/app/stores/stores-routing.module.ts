import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { StoresComponent } from "./stores.component";
import { StoreSalesComponent } from "./sales/sales.component";
import { StoreStaffComponent } from "./staff/staff.component";

const routes: Routes = [
  {
    path: "", component: StoresComponent, children: [
      { path: "sales", component: StoreSalesComponent },
      { path: "staff", component: StoreStaffComponent },
      { path: "", redirectTo: "sales", pathMatch: "full" }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class StoresRoutingModule {
}
