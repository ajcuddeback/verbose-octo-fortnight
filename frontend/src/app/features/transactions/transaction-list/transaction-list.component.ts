import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { TransactionStore } from '../../../core/store/transaction.store';
import { Transaction } from '../../../core/models/transaction.model';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TransactionFormComponent, ConfirmDialogComponent, LoadingSpinnerComponent],
  templateUrl: './transaction-list.component.html',
  styleUrl: './transaction-list.component.css'
})
export class TransactionListComponent implements OnInit {
  private transactionService = inject(TransactionService);
  readonly transactionStore = inject(TransactionStore);

  readonly showForm = signal(false);
  readonly editingTransaction = signal<Transaction | null>(null);
  readonly deletingTransaction = signal<Transaction | null>(null);
  readonly deleteLoading = signal(false);

  // Filter state
  readonly filterType = signal<'' | 'INCOME' | 'EXPENSE'>('');
  readonly filterFrom = signal('');
  readonly filterTo = signal('');
  readonly filterSearch = signal('');
  readonly currentPage = signal(0);
  readonly pageSize = 10;

  readonly loading = this.transactionStore.loading;
  readonly transactions = this.transactionStore.transactions;
  readonly totalIncome = this.transactionStore.totalIncome;
  readonly totalExpenses = this.transactionStore.totalExpenses;
  readonly totalPages = this.transactionStore.totalPages;
  readonly totalElements = this.transactionStore.totalElements;

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.transactionStore.setLoading(true);
    this.transactionService.getTransactions({
      type: this.filterType() || undefined,
      from: this.filterFrom() || undefined,
      to: this.filterTo() || undefined,
      search: this.filterSearch() || undefined,
      page: this.currentPage(),
      size: this.pageSize
    }).subscribe({
      next: (res) => {
        this.transactionStore.setTransactions(res.content);
        this.transactionStore.setPagination(res.totalElements, res.totalPages);
        this.transactionStore.setLoading(false);
      },
      error: () => this.transactionStore.setLoading(false)
    });
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadTransactions();
  }

  clearFilters(): void {
    this.filterType.set('');
    this.filterFrom.set('');
    this.filterTo.set('');
    this.filterSearch.set('');
    this.currentPage.set(0);
    this.loadTransactions();
  }

  openAddForm(): void {
    this.editingTransaction.set(null);
    this.showForm.set(true);
  }

  openEditForm(tx: Transaction): void {
    this.editingTransaction.set(tx);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingTransaction.set(null);
  }

  onSaved(tx: Transaction): void {
    this.closeForm();
    this.loadTransactions();
  }

  confirmDelete(tx: Transaction): void {
    this.deletingTransaction.set(tx);
  }

  deleteTransaction(): void {
    const tx = this.deletingTransaction();
    if (!tx) return;
    this.deleteLoading.set(true);
    this.transactionService.deleteTransaction(tx.id).subscribe({
      next: () => {
        this.transactionStore.removeTransaction(tx.id);
        this.deletingTransaction.set(null);
        this.deleteLoading.set(false);
        this.loadTransactions();
      },
      error: () => {
        this.deleteLoading.set(false);
        this.deletingTransaction.set(null);
      }
    });
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
      this.loadTransactions();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update(p => p + 1);
      this.loadTransactions();
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
