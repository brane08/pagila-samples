import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { ChartData, ChartOptions } from 'chart.js';
import { FilmsService } from '../shared/services/films.service';
import { StoresService } from '../shared/services/stores.service';
import { SalesByFilmCategory } from '../shared/models/films';
import { SalesByStore } from '../shared/models/stores';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class HomeComponent {
  private films = inject(FilmsService);
  private stores = inject(StoresService);

  salesByCategoryResource = rxResource<SalesByFilmCategory[], void>({
    stream: () => this.films.getSalesByCategory().pipe(map(r => r.data))
  });

  salesByStoreResource = rxResource<SalesByStore[], void>({
    stream: () => this.stores.getSalesByStore().pipe(map(r => r.data))
  });

  totalRevenue = computed(() =>
    (this.salesByCategoryResource.value() ?? [])
      .reduce((sum, s) => sum + Number(s.totalSales), 0)
  );

  categoryChartData = computed<ChartData<'bar'>>(() => {
    const sales = [...(this.salesByCategoryResource.value() ?? [])]
      .sort((a, b) => Number(b.totalSales) - Number(a.totalSales));
    return {
      labels: sales.map(s => s.category),
      datasets: [{
        data: sales.map(s => Number(s.totalSales)),
        label: 'Revenue',
        backgroundColor: 'rgba(0, 92, 187, 0.75)',
        borderColor: 'rgba(0, 92, 187, 1)',
        borderWidth: 1,
        borderRadius: 4,
      }]
    };
  });

  storeChartData = computed<ChartData<'doughnut'>>(() => {
    const stores = this.salesByStoreResource.value() ?? [];
    return {
      labels: stores.map(s => s.store),
      datasets: [{
        data: stores.map(s => Number(s.totalSales)),
        backgroundColor: ['rgba(0, 92, 187, 0.8)', 'rgba(255, 152, 0, 0.8)'],
        borderColor: ['#005cbb', '#ff9800'],
        borderWidth: 2,
        hoverOffset: 8,
      }]
    };
  });

  categoryBarOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      title: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => ` $${Number(ctx.raw).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
        }
      }
    },
    scales: {
      x: {
        ticks: {
          callback: v => `$${Number(v).toLocaleString('en-US', { maximumFractionDigits: 0 })}`
        },
        grid: { color: 'rgba(0,0,0,0.05)' }
      },
      y: { grid: { display: false } }
    }
  };

  storeDoughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' },
      tooltip: {
        callbacks: {
          label: ctx => ` $${Number(ctx.raw).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
        }
      }
    }
  };
}
