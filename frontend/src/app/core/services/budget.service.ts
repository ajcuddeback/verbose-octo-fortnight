import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BudgetItem, BudgetRequest, BudgetSummary } from '../models/budget.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private http = inject(HttpClient);

  getBudgets(month: number, year: number): Observable<BudgetItem[]> {
    const params = new HttpParams().set('month', month.toString()).set('year', year.toString());
    return this.http.get<BudgetItem[]>(`${API_BASE}/budgets`, { params });
  }

  getBudgetSummary(month: number, year: number): Observable<BudgetSummary> {
    const params = new HttpParams().set('month', month.toString()).set('year', year.toString());
    return this.http.get<BudgetSummary>(`${API_BASE}/budgets/summary`, { params });
  }

  createBudget(req: BudgetRequest): Observable<BudgetItem> {
    return this.http.post<BudgetItem>(`${API_BASE}/budgets`, req);
  }

  updateBudget(id: number, req: BudgetRequest): Observable<BudgetItem> {
    return this.http.put<BudgetItem>(`${API_BASE}/budgets/${id}`, req);
  }

  deleteBudget(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/budgets/${id}`);
  }
}
