import { ChartData } from "chart.js";
import * as Utils from "./utils";
import { randomArray } from "../models/charts.model";
import { Injectable } from "@angular/core";

@Injectable({ providedIn: "root" })
export class ChartDataService {

  randomStackedOSs(): ChartData<'bar'> {
    return {
      datasets: [
        { data: randomArray(), label: "RHEL 6", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "RHEL 7", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "RHEL 8", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "RHEL 9", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Monterey", stack: "macOS", minBarLength: 16, maxBarThickness: 16 },
        { data: randomArray(), label: "Ventura", stack: "macOS", minBarLength: 16, maxBarThickness: 16 },
        { data: randomArray(), label: "Sonoma", stack: "macOS", minBarLength: 16, maxBarThickness: 16 },
        { data: randomArray(), label: "Sequoia", stack: "macOS", minBarLength: 16, maxBarThickness: 16 },
      ],
      labels: Utils.months({ count: 4 })
    };
  }

  randomStackedOSVul(): ChartData<'bar'> {
    return {
      datasets: [
        { data: randomArray(), label: "Aging", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Current", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Next", stack: "Red Hat", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Aging", stack: "macOS", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Current", stack: "macOS", minBarLength: 16, maxBarThickness: 32 },
        { data: randomArray(), label: "Next", stack: "macOS", minBarLength: 16, maxBarThickness: 32 },
      ],
      labels: Utils.months({ count: 4 })
    };
  }
}
