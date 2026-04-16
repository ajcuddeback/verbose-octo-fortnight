import { Injectable, signal, computed } from '@angular/core';
import { Transaction } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionStore {
  private _transactions = signal<Transaction[]>([]);
  private _loading = signal(false);
  private _filter = signal<{ type?: string; from?: string; to?: string; search?: string }>({});
  private _currentPage = signal(0);
  private _totalElements = signal(0);
  private _totalPages = signal(0);

  readonly transactions = this._transactions.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly filter = this._filter.asReadonly();
  readonly currentPage = this._currentPage.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly totalPages = this._totalPages.asReadonly();

  readonly totalIncome = computed(() =>
    this._transactions().filter(t => t.type === 'INCOME').reduce((s, t) => s + t.amount, 0)
  );

  readonly totalExpenses = computed(() =>
    this._transactions().filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount, 0)
  );

  readonly netSavings = computed(() => this.totalIncome() - this.totalExpenses());

  readonly savingsRate = computed(() => {
    const income = this.totalIncome();
    if (income === 0) return 0;
    return (this.netSavings() / income) * 100;
  });

  setTransactions(t: Transaction[]): void { this._transactions.set(t); }
  addTransaction(t: Transaction): void { this._transactions.update(prev => [t, ...prev]); }
  updateTransaction(t: Transaction): void { this._transactions.update(prev => prev.map(x => x.id === t.id ? t : x)); }
  removeTransaction(id: number): void { this._transactions.update(prev => prev.filter(x => x.id !== id)); }
  setLoading(v: boolean): void { this._loading.set(v); }
  setFilter(f: { type?: string; from?: string; to?: string; search?: string }): void { this._filter.set(f); }
  setPage(page: number): void { this._currentPage.set(page); }
  setPagination(totalElements: number, totalPages: number): void {
    this._totalElements.set(totalElements);
    this._totalPages.set(totalPages);
  }
}
