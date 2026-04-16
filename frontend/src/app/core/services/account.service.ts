import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account, AccountRequest } from '../models/account.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);

  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${API_BASE}/accounts`);
  }

  getAccount(id: number): Observable<Account> {
    return this.http.get<Account>(`${API_BASE}/accounts/${id}`);
  }

  createAccount(req: AccountRequest): Observable<Account> {
    return this.http.post<Account>(`${API_BASE}/accounts`, req);
  }

  updateAccount(id: number, req: AccountRequest): Observable<Account> {
    return this.http.put<Account>(`${API_BASE}/accounts/${id}`, req);
  }

  deleteAccount(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/accounts/${id}`);
  }
}
