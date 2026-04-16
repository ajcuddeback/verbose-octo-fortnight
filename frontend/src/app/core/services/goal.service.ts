import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Goal, GoalRequest, GoalContribution } from '../models/goal.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class GoalService {
  private http = inject(HttpClient);

  getGoals(): Observable<Goal[]> {
    return this.http.get<Goal[]>(`${API_BASE}/goals`);
  }

  getGoal(id: number): Observable<Goal> {
    return this.http.get<Goal>(`${API_BASE}/goals/${id}`);
  }

  createGoal(req: GoalRequest): Observable<Goal> {
    return this.http.post<Goal>(`${API_BASE}/goals`, req);
  }

  updateGoal(id: number, req: GoalRequest): Observable<Goal> {
    return this.http.put<Goal>(`${API_BASE}/goals/${id}`, req);
  }

  deleteGoal(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/goals/${id}`);
  }

  contributeToGoal(id: number, amount: number, note?: string): Observable<Goal> {
    const contribution: GoalContribution = { amount, note };
    return this.http.post<Goal>(`${API_BASE}/goals/${id}/contribute`, contribution);
  }
}
