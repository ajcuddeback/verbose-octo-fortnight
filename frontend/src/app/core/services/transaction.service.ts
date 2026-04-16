import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, TransactionRequest, TransactionFilter, PagedResponse } from '../models/transaction.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);

  getTransactions(filter?: TransactionFilter): Observable<PagedResponse<Transaction>> {
    let params = new HttpParams();
    if (filter) {
      if (filter.type) params = params.set('type', filter.type);
      if (filter.from) params = params.set('from', filter.from);
      if (filter.to) params = params.set('to', filter.to);
      if (filter.search) params = params.set('search', filter.search);
      if (filter.page !== undefined) params = params.set('page', filter.page.toString());
      if (filter.size !== undefined) params = params.set('size', filter.size.toString());
    }
    return this.http.get<PagedResponse<Transaction>>(`${API_BASE}/transactions`, { params });
  }

  getTransaction(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${API_BASE}/transactions/${id}`);
  }

  createTransaction(req: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${API_BASE}/transactions`, req);
  }

  updateTransaction(id: number, req: TransactionRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`${API_BASE}/transactions/${id}`, req);
  }

  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/transactions/${id}`);
  }
}
