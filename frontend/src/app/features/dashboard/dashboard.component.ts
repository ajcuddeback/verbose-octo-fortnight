import {
  Component, inject, signal, computed, OnInit, OnDestroy,
  ViewChild, ElementRef, AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { TransactionService } from '../../core/services/transaction.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { BillService } from '../../core/services/bill.service';
import { TransactionStore } from '../../core/store/transaction.store';
import { AnalyticsStore } from '../../core/store/analytics.store';
import { Transaction } from '../../core/models/transaction.model';
import { Bill } from '../../core/models/bill.model';
import { FinancialHealthScore } from '../../core/models/analytics.model';
import { SpendingTrend } from '../../core/models/analytics.model';
import { CategoryBreakdown } from '../../core/models/analytics.model';
import { FinancialHealthScoreComponent } from '../../shared/components/financial-health-score/financial-health-score.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FinancialHealthScoreComponent, LoadingSpinnerComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private transactionService = inject(TransactionService);
  private analyticsService = inject(AnalyticsService);
  private billService = inject(BillService);
  private transactionStore = inject(TransactionStore);
  private analyticsStore = inject(AnalyticsStore);

  @ViewChild('incomeExpenseChart') incomeExpenseChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryChart') categoryChartRef!: ElementRef<HTMLCanvasElement>;

  private incomeExpenseChart: Chart | null = null;
  private categoryChart: Chart | null = null;

  readonly loading = signal(true);
  readonly recentTransactions = signal<Transaction[]>([]);
  readonly upcomingBills = signal<Bill[]>([]);
  readonly healthScore = signal<FinancialHealthScore | null>(null);
  readonly trends = signal<SpendingTrend[]>([]);
  readonly categoryBreakdown = signal<CategoryBreakdown[]>([]);

  readonly totalIncome = this.transactionStore.totalIncome;
  readonly totalExpenses = this.transactionStore.totalExpenses;
  readonly netSavings = this.transactionStore.netSavings;
  readonly savingsRate = computed(() => {
    const income = this.totalIncome();
    if (income === 0) return 0;
    return Math.round((this.netSavings() / income) * 100);
  });

  private chartsInitialized = false;

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    if (!this.loading()) {
      this.initCharts();
    }
  }

  ngOnDestroy(): void {
    this.incomeExpenseChart?.destroy();
    this.categoryChart?.destroy();
  }

  private loadDashboardData(): void {
    this.loading.set(true);
    const now = new Date();
    const month = now.getMonth() + 1;
    const year = now.getFullYear();

    let completed = 0;
    const checkDone = () => {
      completed++;
      if (completed >= 4) {
        this.loading.set(false);
        setTimeout(() => this.initCharts(), 0);
      }
    };

    this.transactionService.getTransactions({ size: 5 }).subscribe({
      next: (res) => {
        this.transactionStore.setTransactions(res.content);
        this.recentTransactions.set(res.content.slice(0, 5));
        checkDone();
      },
      error: () => checkDone()
    });

    this.analyticsService.getFinancialHealth().subscribe({
      next: (score) => {
        this.healthScore.set(score);
        this.analyticsStore.setHealthScore(score);
        checkDone();
      },
      error: () => checkDone()
    });

    this.analyticsService.getSpendingTrends(6).subscribe({
      next: (trends) => {
        this.trends.set(trends);
        checkDone();
      },
      error: () => checkDone()
    });

    this.analyticsService.getCategoryBreakdown(month, year).subscribe({
      next: (breakdown) => {
        this.categoryBreakdown.set(breakdown);
        checkDone();
      },
      error: () => checkDone()
    });

    this.billService.getBills().subscribe({
      next: (bills) => {
        const upcoming = bills
          .filter(b => b.status !== 'PAID')
          .sort((a, b) => new Date(a.nextDueDate).getTime() - new Date(b.nextDueDate).getTime())
          .slice(0, 5);
        this.upcomingBills.set(upcoming);
      },
      error: () => {}
    });
  }

  private initCharts(): void {
    if (this.chartsInitialized) return;
    if (!this.incomeExpenseChartRef || !this.categoryChartRef) return;
    this.chartsInitialized = true;

    this.createIncomeExpenseChart();
    this.createCategoryChart();
  }

  private createIncomeExpenseChart(): void {
    const trends = this.trends();
    const labels = trends.map(t => t.month);
    const incomeData = trends.map(t => t.totalIncome);
    const expenseData = trends.map(t => t.totalExpenses);

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: 'rgba(16, 185, 129, 0.7)',
            borderColor: '#10b981',
            borderWidth: 1,
            borderRadius: 4,
          },
          {
            label: 'Expenses',
            data: expenseData,
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: '#ef4444',
            borderWidth: 1,
            borderRadius: 4,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { labels: { color: '#94a3b8', font: { size: 12 } } },
          tooltip: { callbacks: { label: ctx => ` $${ctx.parsed.y.toLocaleString()}` } }
        },
        scales: {
          x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(51,65,85,0.5)' } },
          y: {
            ticks: { color: '#94a3b8', callback: (v) => `$${Number(v).toLocaleString()}` },
            grid: { color: 'rgba(51,65,85,0.5)' }
          }
        }
      }
    };

    this.incomeExpenseChart = new Chart(this.incomeExpenseChartRef.nativeElement, config);
  }

  private createCategoryChart(): void {
    const breakdown = this.categoryBreakdown();
    const labels = breakdown.map(c => c.categoryName);
    const data = breakdown.map(c => c.totalAmount);
    const colors = breakdown.map(c => c.categoryColor || '#6366f1');

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: colors.map(c => c + 'cc'),
          borderColor: colors,
          borderWidth: 2,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'right',
            labels: { color: '#94a3b8', font: { size: 11 }, padding: 16 }
          },
          tooltip: {
            callbacks: {
              label: ctx => {
                const b = breakdown[ctx.dataIndex];
                return ` $${ctx.parsed.toLocaleString()} (${b.percentage.toFixed(1)}%)`;
              }
            }
          }
        }
      }
    };

    this.categoryChart = new Chart(this.categoryChartRef.nativeElement, config);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  getBillStatus(bill: Bill): string {
    const today = new Date();
    const due = new Date(bill.nextDueDate);
    const diffDays = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return 'overdue';
    if (diffDays <= 7) return 'soon';
    return 'ok';
  }

  getBillStatusLabel(bill: Bill): string {
    const status = this.getBillStatus(bill);
    if (status === 'overdue') return 'Overdue';
    const due = new Date(bill.nextDueDate);
    const today = new Date();
    const diffDays = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (status === 'soon') return `Due in ${diffDays}d`;
    return this.formatDate(bill.nextDueDate);
  }
}
