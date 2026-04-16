import { Injectable, signal } from '@angular/core';
import {
  SpendingTrend,
  CategoryBreakdown,
  Prediction,
  FinancialHealthScore,
  HealthScoreHistory,
  SpendingDna
} from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsStore {
  private _trends = signal<SpendingTrend[]>([]);
  private _categoryBreakdown = signal<CategoryBreakdown[]>([]);
  private _predictions = signal<Prediction[]>([]);
  private _healthScore = signal<FinancialHealthScore | null>(null);
  private _healthScoreHistory = signal<HealthScoreHistory[]>([]);
  private _spendingDna = signal<SpendingDna | null>(null);
  private _loading = signal(false);

  readonly trends = this._trends.asReadonly();
  readonly categoryBreakdown = this._categoryBreakdown.asReadonly();
  readonly predictions = this._predictions.asReadonly();
  readonly healthScore = this._healthScore.asReadonly();
  readonly healthScoreHistory = this._healthScoreHistory.asReadonly();
  readonly spendingDna = this._spendingDna.asReadonly();
  readonly loading = this._loading.asReadonly();

  setTrends(data: SpendingTrend[]): void { this._trends.set(data); }
  setCategoryBreakdown(data: CategoryBreakdown[]): void { this._categoryBreakdown.set(data); }
  setPredictions(data: Prediction[]): void { this._predictions.set(data); }
  setHealthScore(data: FinancialHealthScore): void { this._healthScore.set(data); }
  setHealthScoreHistory(data: HealthScoreHistory[]): void { this._healthScoreHistory.set(data); }
  setSpendingDna(data: SpendingDna): void { this._spendingDna.set(data); }
  setLoading(v: boolean): void { this._loading.set(v); }
}
