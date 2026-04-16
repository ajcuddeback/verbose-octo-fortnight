import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SpendingTrend,
  CategoryBreakdown,
  Prediction,
  FinancialHealthScore,
  HealthScoreHistory,
  SpendingDna
} from '../models/analytics.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);

  getSpendingTrends(months: number = 12): Observable<SpendingTrend[]> {
    const params = new HttpParams().set('months', months.toString());
    return this.http.get<SpendingTrend[]>(`${API_BASE}/analytics/spending-trends`, { params });
  }

  getCategoryBreakdown(month: number, year: number): Observable<CategoryBreakdown[]> {
    const params = new HttpParams().set('month', month.toString()).set('year', year.toString());
    return this.http.get<CategoryBreakdown[]>(`${API_BASE}/analytics/category-breakdown`, { params });
  }

  getPredictions(): Observable<Prediction[]> {
    return this.http.get<Prediction[]>(`${API_BASE}/analytics/predictions`);
  }

  getSpendingDna(): Observable<SpendingDna> {
    return this.http.get<SpendingDna>(`${API_BASE}/analytics/spending-dna`);
  }

  getFinancialHealth(): Observable<FinancialHealthScore> {
    return this.http.get<FinancialHealthScore>(`${API_BASE}/analytics/financial-health`);
  }

  getHealthScoreHistory(months: number = 12): Observable<HealthScoreHistory[]> {
    const params = new HttpParams().set('months', months.toString());
    return this.http.get<HealthScoreHistory[]>(`${API_BASE}/analytics/health-score-history`, { params });
  }
}
