import { Component, OnInit } from '@angular/core';
import { ChartDataService } from "../shared/services/chartdata.service";
import { ChartConfiguration, ChartData } from "chart.js";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: false
})
export class HomeComponent implements OnInit {

  options!: ChartConfiguration<'bar'>['options'];
  data!: ChartData<'bar'>;

  constructor(private chartData: ChartDataService) {
  }

  ngOnInit(): void {
    this.options = {
      // We use these empty structures as placeholders for dynamic theming.
      scales: {
        x: {
          stacked: true,
        },
        y: {
          stacked: true,
          min: 10,
        },
      },
      plugins: {
        title: {
          display: true,
          text: "This could be stacked"
        }
      },
    };
    this.data = this.chartData.randomStackedOSVul();
  }


}
