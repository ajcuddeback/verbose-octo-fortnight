import {
  Component, inject, signal, OnInit, OnDestroy,
  ViewChild, ElementRef, AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsStore } from '../../core/store/analytics.store';
import { SpendingTrend, CategoryBreakdown, Prediction, FinancialHealthScore, HealthScoreHistory, SpendingDna } from '../../core/models/analytics.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

Chart.register(...registerables);

type Tab = 'trends' | 'categories' | 'predictions' | 'health' | 'dna' | 'debt';

interface DebtItem {
  name: string;
  balance: number;
  interestRate: number;
  minPayment: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingSpinnerComponent],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.css'
})
export class AnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {
  private analyticsService = inject(AnalyticsService);
  private analyticsStore = inject(AnalyticsStore);

  @ViewChild('trendsChart') trendsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryChart') categoryChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('predictionsChart') predictionsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('healthHistoryChart') healthHistoryChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('debtChart') debtChartRef!: ElementRef<HTMLCanvasElement>;

  private charts: Map<string, Chart> = new Map();

  readonly activeTab = signal<Tab>('trends');
  readonly loading = signal(false);
  readonly trends = signal<SpendingTrend[]>([]);
  readonly categoryBreakdown = signal<CategoryBreakdown[]>([]);
  readonly predictions = signal<Prediction[]>([]);
  readonly healthScore = signal<FinancialHealthScore | null>(null);
  readonly healthHistory = signal<HealthScoreHistory[]>([]);
  readonly spendingDna = signal<SpendingDna | null>(null);

  // Debt Calculator
  readonly debtItems = signal<DebtItem[]>([
    { name: 'Credit Card', balance: 5000, interestRate: 18.99, minPayment: 150 }
  ]);
  readonly extraPayment = signal(100);
  readonly debtMethod = signal<'avalanche' | 'snowball'>('avalanche');

  readonly now = new Date();
  readonly currentMonth = this.now.getMonth() + 1;
  readonly currentYear = this.now.getFullYear();

  ngOnInit(): void {
    this.loadAll();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.charts.forEach(c => c.destroy());
    this.charts.clear();
  }

  private loadAll(): void {
    this.loading.set(true);
    let done = 0;
    const total = 5;
    const check = () => { done++; if (done >= total) { this.loading.set(false); setTimeout(() => this.renderActiveChart(), 50); }};

    this.analyticsService.getSpendingTrends(12).subscribe({ next: (d) => { this.trends.set(d); check(); }, error: check });
    this.analyticsService.getCategoryBreakdown(this.currentMonth, this.currentYear).subscribe({ next: (d) => { this.categoryBreakdown.set(d); check(); }, error: check });
    this.analyticsService.getPredictions().subscribe({ next: (d) => { this.predictions.set(d); check(); }, error: check });
    this.analyticsService.getHealthScoreHistory(12).subscribe({ next: (d) => { this.healthHistory.set(d); check(); }, error: check });
    this.analyticsService.getSpendingDna().subscribe({ next: (d) => { this.spendingDna.set(d); check(); }, error: check });
    this.analyticsService.getFinancialHealth().subscribe({ next: (d) => this.healthScore.set(d), error: () => {} });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    setTimeout(() => this.renderActiveChart(), 50);
  }

  private renderActiveChart(): void {
    switch (this.activeTab()) {
      case 'trends': this.renderTrendsChart(); break;
      case 'categories': this.renderCategoryChart(); break;
      case 'predictions': this.renderPredictionsChart(); break;
      case 'health': this.renderHealthHistoryChart(); break;
      case 'debt': this.renderDebtChart(); break;
    }
  }

  private destroyChart(key: string): void {
    const existing = this.charts.get(key);
    if (existing) { existing.destroy(); this.charts.delete(key); }
  }

  private renderTrendsChart(): void {
    if (!this.trendsChartRef) return;
    this.destroyChart('trends');
    const t = this.trends();
    const cfg: ChartConfiguration = {
      type: 'line',
      data: {
        labels: t.map(x => x.month),
        datasets: [
          { label: 'Income', data: t.map(x => x.totalIncome), borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.1)', fill: true, tension: 0.4, pointBackgroundColor: '#10b981', pointRadius: 4 },
          { label: 'Expenses', data: t.map(x => x.totalExpenses), borderColor: '#ef4444', backgroundColor: 'rgba(239,68,68,0.1)', fill: true, tension: 0.4, pointBackgroundColor: '#ef4444', pointRadius: 4 },
          { label: 'Net Savings', data: t.map(x => x.netSavings), borderColor: '#6366f1', backgroundColor: 'rgba(99,102,241,0.1)', fill: false, tension: 0.4, pointBackgroundColor: '#6366f1', pointRadius: 4, borderDash: [4,4] }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' } }, tooltip: { callbacks: { label: ctx => ` $${ctx.parsed.y.toLocaleString()}` }}},
        scales: {
          x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(51,65,85,0.5)' }},
          y: { ticks: { color: '#94a3b8', callback: v => `$${Number(v).toLocaleString()}` }, grid: { color: 'rgba(51,65,85,0.5)' }}
        }
      }
    };
    this.charts.set('trends', new Chart(this.trendsChartRef.nativeElement, cfg));
  }

  private renderCategoryChart(): void {
    if (!this.categoryChartRef) return;
    this.destroyChart('categories');
    const c = this.categoryBreakdown();
    const cfg: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: c.map(x => x.categoryName),
        datasets: [{ data: c.map(x => x.totalAmount), backgroundColor: c.map(x => (x.categoryColor || '#6366f1') + 'cc'), borderColor: c.map(x => x.categoryColor || '#6366f1'), borderWidth: 2, hoverOffset: 8 }]
      },
      options: {
        responsive: true, maintainAspectRatio: false, cutout: '65%',
        plugins: { legend: { position: 'right', labels: { color: '#94a3b8', padding: 14 }}, tooltip: { callbacks: { label: ctx => { const item = c[ctx.dataIndex]; return ` $${ctx.parsed.toLocaleString()} (${item.percentage.toFixed(1)}%)`; }}}}
      }
    };
    this.charts.set('categories', new Chart(this.categoryChartRef.nativeElement, cfg));
  }

  private renderPredictionsChart(): void {
    if (!this.predictionsChartRef) return;
    this.destroyChart('predictions');
    const p = this.predictions().slice(0, 3);
    const cfg: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: p.map(x => `${x.month} ${x.year}`),
        datasets: [
          { label: 'Predicted Income', data: p.map(x => x.predictedIncome), backgroundColor: 'rgba(16,185,129,0.7)', borderColor: '#10b981', borderWidth: 1, borderRadius: 4 },
          { label: 'Predicted Expenses', data: p.map(x => x.predictedExpenses), backgroundColor: 'rgba(239,68,68,0.7)', borderColor: '#ef4444', borderWidth: 1, borderRadius: 4 },
          { label: 'Predicted Savings', data: p.map(x => x.predictedSavings), backgroundColor: 'rgba(99,102,241,0.7)', borderColor: '#6366f1', borderWidth: 1, borderRadius: 4 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' }}, tooltip: { callbacks: { label: ctx => ` $${ctx.parsed.y.toLocaleString()}` }}},
        scales: {
          x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(51,65,85,0.5)' }},
          y: { ticks: { color: '#94a3b8', callback: v => `$${Number(v).toLocaleString()}` }, grid: { color: 'rgba(51,65,85,0.5)' }}
        }
      }
    };
    this.charts.set('predictions', new Chart(this.predictionsChartRef.nativeElement, cfg));
  }

  private renderHealthHistoryChart(): void {
    if (!this.healthHistoryChartRef) return;
    this.destroyChart('health');
    const h = this.healthHistory();
    const cfg: ChartConfiguration = {
      type: 'line',
      data: {
        labels: h.map(x => new Date(x.date).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })),
        datasets: [{ label: 'Health Score', data: h.map(x => x.score), borderColor: '#6366f1', backgroundColor: 'rgba(99,102,241,0.15)', fill: true, tension: 0.4, pointBackgroundColor: '#6366f1', pointRadius: 5 }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' }}},
        scales: {
          x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(51,65,85,0.5)' }},
          y: { min: 0, max: 100, ticks: { color: '#94a3b8' }, grid: { color: 'rgba(51,65,85,0.5)' }}
        }
      }
    };
    this.charts.set('health', new Chart(this.healthHistoryChartRef.nativeElement, cfg));
  }

  private renderDebtChart(): void {
    if (!this.debtChartRef) return;
    this.destroyChart('debt');
    const { avalanche, snowball } = this.calculatePayoff();
    const maxMonths = Math.max(...avalanche.map(d => d.months), ...snowball.map(d => d.months));
    const labels = Array.from({ length: maxMonths + 1 }, (_, i) => `Month ${i}`);

    const cfg: ChartConfiguration = {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Avalanche Method', data: avalanche.map(d => d.totalBalance), borderColor: '#6366f1', backgroundColor: 'transparent', tension: 0.4, pointRadius: 0 },
          { label: 'Snowball Method', data: snowball.map(d => d.totalBalance), borderColor: '#f59e0b', backgroundColor: 'transparent', tension: 0.4, pointRadius: 0, borderDash: [6,3] }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' }}, tooltip: { callbacks: { label: ctx => ` $${ctx.parsed.y.toLocaleString()}` }}},
        scales: {
          x: { ticks: { color: '#94a3b8', maxTicksLimit: 12 }, grid: { color: 'rgba(51,65,85,0.5)' }},
          y: { ticks: { color: '#94a3b8', callback: v => `$${Number(v).toLocaleString()}` }, grid: { color: 'rgba(51,65,85,0.5)' }}
        }
      }
    };
    this.charts.set('debt', new Chart(this.debtChartRef.nativeElement, cfg));
  }

  calculatePayoff(): { avalanche: { month: number; totalBalance: number; months: number }[]; snowball: { month: number; totalBalance: number; months: number }[] } {
    const extra = this.extraPayment();
    const items = this.debtItems().filter(d => d.balance > 0);

    const simulate = (sortFn: (a: DebtItem, b: DebtItem) => number) => {
      let debts = items.map(d => ({ ...d }));
      const data: { month: number; totalBalance: number; months: number }[] = [];
      let month = 0;
      while (debts.some(d => d.balance > 0) && month < 360) {
        const total = debts.reduce((s, d) => s + Math.max(0, d.balance), 0);
        data.push({ month, totalBalance: total, months: month });
        debts.sort(sortFn);
        let extraLeft = extra;
        debts = debts.map(d => {
          if (d.balance <= 0) return d;
          const interest = d.balance * (d.interestRate / 100 / 12);
          let payment = d.minPayment;
          if (debts.indexOf(d) === 0) payment += extraLeft;
          const newBalance = Math.max(0, d.balance + interest - payment);
          if (newBalance === 0) extraLeft += d.minPayment;
          return { ...d, balance: newBalance };
        });
        month++;
      }
      data.push({ month, totalBalance: 0, months: month });
      return data;
    };

    return {
      avalanche: simulate((a, b) => b.interestRate - a.interestRate),
      snowball: simulate((a, b) => a.balance - b.balance)
    };
  }

  addDebt(): void {
    this.debtItems.update(items => [...items, { name: 'New Debt', balance: 1000, interestRate: 15, minPayment: 50 }]);
  }

  removeDebt(i: number): void {
    this.debtItems.update(items => items.filter((_, idx) => idx !== i));
  }

  updateDebt(i: number, field: keyof DebtItem, value: string | number): void {
    this.debtItems.update(items => items.map((item, idx) => idx === i ? { ...item, [field]: field === 'name' ? value : Number(value) } : item));
  }

  recalcDebt(): void {
    this.destroyChart('debt');
    setTimeout(() => this.renderDebtChart(), 50);
  }

  getAvalancheMonths(): number {
    const { avalanche } = this.calculatePayoff();
    return avalanche.length - 1;
  }

  getSnowballMonths(): number {
    const { snowball } = this.calculatePayoff();
    return snowball.length - 1;
  }

  getFirstPrediction(): Prediction | null {
    return this.predictions()[0] ?? null;
  }

  getConfidenceClass(score: number): string {
    if (score >= 0.8) return 'text-green';
    if (score >= 0.6) return 'text-yellow';
    return 'text-red';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }
}
