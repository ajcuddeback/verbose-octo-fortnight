import { Component, inject, signal, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransactionService } from '../../../core/services/transaction.service';
import { CategoryService } from '../../../core/services/category.service';
import { AccountService } from '../../../core/services/account.service';
import { TransactionStore } from '../../../core/store/transaction.store';
import { Transaction, TransactionRequest } from '../../../core/models/transaction.model';
import { Category } from '../../../core/models/category.model';
import { Account } from '../../../core/models/account.model';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transaction-form.component.html',
  styleUrl: './transaction-form.component.css'
})
export class TransactionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private categoryService = inject(CategoryService);
  private accountService = inject(AccountService);
  private transactionStore = inject(TransactionStore);

  @Input() transaction: Transaction | null = null;
  @Output() saved = new EventEmitter<Transaction>();
  @Output() cancelled = new EventEmitter<void>();

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);

  form = this.fb.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    description: ['', [Validators.required, Validators.maxLength(200)]],
    transactionDate: [new Date().toISOString().split('T')[0], Validators.required],
    type: ['EXPENSE' as 'INCOME' | 'EXPENSE', Validators.required],
    categoryId: [null as number | null],
    accountId: [null as number | null],
    notes: [''],
    tags: ['']
  });

  ngOnInit(): void {
    if (this.transaction) {
      this.form.patchValue({
        amount: this.transaction.amount,
        description: this.transaction.description,
        transactionDate: this.transaction.transactionDate.split('T')[0],
        type: this.transaction.type,
        categoryId: this.transaction.categoryId ?? null,
        accountId: this.transaction.accountId ?? null,
        notes: this.transaction.notes ?? '',
        tags: this.transaction.tags ?? ''
      });
    }

    this.categoryService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {}
    });

    this.accountService.getAccounts().subscribe({
      next: (accs) => this.accounts.set(accs),
      error: () => {}
    });
  }

  get filteredCategories(): Category[] {
    const type = this.form.get('type')?.value;
    return this.categories().filter(c => c.type === type || c.type === 'BOTH');
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const val = this.form.value;
    const req: TransactionRequest = {
      amount: val.amount!,
      description: val.description!,
      transactionDate: val.transactionDate!,
      type: val.type as 'INCOME' | 'EXPENSE',
      categoryId: val.categoryId ?? undefined,
      accountId: val.accountId ?? undefined,
      notes: val.notes || undefined,
      tags: val.tags || undefined
    };

    const obs = this.transaction
      ? this.transactionService.updateTransaction(this.transaction.id, req)
      : this.transactionService.createTransaction(req);

    obs.subscribe({
      next: (tx) => {
        if (this.transaction) {
          this.transactionStore.updateTransaction(tx);
        } else {
          this.transactionStore.addTransaction(tx);
        }
        this.saved.emit(tx);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to save transaction.');
      }
    });
  }
}
