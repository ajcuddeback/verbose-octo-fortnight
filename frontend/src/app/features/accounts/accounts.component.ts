import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { Account, AccountRequest, AccountType } from '../../core/models/account.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

const ACCOUNT_TYPES: { value: AccountType; label: string }[] = [
  { value: 'CHECKING', label: 'Checking' },
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'CREDIT_CARD', label: 'Credit Card' },
  { value: 'INVESTMENT', label: 'Investment' },
  { value: 'LOAN', label: 'Loan' },
  { value: 'OTHER', label: 'Other' },
];

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ConfirmDialogComponent, LoadingSpinnerComponent],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.css'
})
export class AccountsComponent implements OnInit {
  private accountService = inject(AccountService);
  private fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly accounts = signal<Account[]>([]);
  readonly showForm = signal(false);
  readonly editingAccount = signal<Account | null>(null);
  readonly deletingAccount = signal<Account | null>(null);
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly accountTypes = ACCOUNT_TYPES;

  readonly netWorth = computed(() => {
    return this.accounts().reduce((sum, acc) => {
      if (acc.type === 'CREDIT_CARD' || acc.type === 'LOAN') return sum - acc.balance;
      return sum + acc.balance;
    }, 0);
  });

  readonly totalAssets = computed(() => {
    return this.accounts()
      .filter(a => a.type !== 'CREDIT_CARD' && a.type !== 'LOAN')
      .reduce((s, a) => s + a.balance, 0);
  });

  readonly totalLiabilities = computed(() => {
    return this.accounts()
      .filter(a => a.type === 'CREDIT_CARD' || a.type === 'LOAN')
      .reduce((s, a) => s + a.balance, 0);
  });

  accountForm = this.fb.group({
    name: ['', Validators.required],
    bankName: ['', Validators.required],
    type: ['CHECKING' as AccountType, Validators.required],
    balance: [0, Validators.required],
    accountNumberLast4: ['', [Validators.pattern(/^\d{4}$/)]]
  });

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.accountService.getAccounts().subscribe({
      next: (accs) => {
        this.accounts.set(accs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.editingAccount.set(null);
    this.accountForm.reset({ name: '', bankName: '', type: 'CHECKING', balance: 0, accountNumberLast4: '' });
    this.formError.set(null);
    this.showForm.set(true);
  }

  openEditForm(account: Account): void {
    this.editingAccount.set(account);
    this.accountForm.patchValue({
      name: account.name,
      bankName: account.bankName,
      type: account.type,
      balance: account.balance,
      accountNumberLast4: account.accountNumberLast4 ?? ''
    });
    this.formError.set(null);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingAccount.set(null);
  }

  submitForm(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }
    this.formLoading.set(true);
    this.formError.set(null);
    const val = this.accountForm.value;
    const req: AccountRequest = {
      name: val.name!,
      bankName: val.bankName!,
      type: val.type! as AccountType,
      balance: val.balance!,
      accountNumberLast4: val.accountNumberLast4 || undefined
    };
    const obs = this.editingAccount()
      ? this.accountService.updateAccount(this.editingAccount()!.id, req)
      : this.accountService.createAccount(req);

    obs.subscribe({
      next: () => {
        this.formLoading.set(false);
        this.closeForm();
        this.loadAccounts();
      },
      error: (err) => {
        this.formLoading.set(false);
        this.formError.set(err.error?.message || 'Failed to save account.');
      }
    });
  }

  confirmDelete(account: Account): void {
    this.deletingAccount.set(account);
  }

  deleteAccount(): void {
    const a = this.deletingAccount();
    if (!a) return;
    this.accountService.deleteAccount(a.id).subscribe({
      next: () => {
        this.deletingAccount.set(null);
        this.loadAccounts();
      },
      error: () => this.deletingAccount.set(null)
    });
  }

  getAccountTypeLabel(type: AccountType): string {
    return ACCOUNT_TYPES.find(t => t.value === type)?.label ?? type;
  }

  getAccountTypeClass(type: AccountType): string {
    switch (type) {
      case 'CHECKING': return 'badge-blue';
      case 'SAVINGS': return 'badge-green';
      case 'CREDIT_CARD': return 'badge-red';
      case 'INVESTMENT': return 'badge-purple';
      case 'LOAN': return 'badge-orange';
      default: return 'badge-gray';
    }
  }

  getBalanceClass(account: Account): string {
    if (account.type === 'CREDIT_CARD' || account.type === 'LOAN') return 'text-red';
    if (account.type === 'INVESTMENT') return 'text-accent';
    return 'text-green';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }
}
