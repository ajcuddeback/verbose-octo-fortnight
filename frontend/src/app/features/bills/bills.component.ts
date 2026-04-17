import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { BillService } from '../../core/services/bill.service';
import { Bill, BillRequest, BillFrequency } from '../../core/models/bill.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

const FREQUENCIES: { value: BillFrequency; label: string }[] = [
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'ANNUALLY', label: 'Annually' },
];

@Component({
  selector: 'app-bills',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ConfirmDialogComponent, LoadingSpinnerComponent],
  templateUrl: './bills.component.html',
  styleUrl: './bills.component.css'
})
export class BillsComponent implements OnInit {
  private billService = inject(BillService);
  private fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly bills = signal<Bill[]>([]);
  readonly showForm = signal(false);
  readonly editingBill = signal<Bill | null>(null);
  readonly deletingBill = signal<Bill | null>(null);
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly frequencies = FREQUENCIES;

  readonly upcomingBills = computed(() => {
    const today = new Date();
    const in30Days = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000);
    return this.bills()
      .filter(b => {
        const due = new Date(b.nextDueDate);
        return due <= in30Days;
      })
      .sort((a, b) => new Date(a.nextDueDate).getTime() - new Date(b.nextDueDate).getTime());
  });

  readonly monthlyTotal = computed(() => {
    return this.bills().filter(b => !this.isBillPaidThisCycle(b)).reduce((sum, b) => {
      if (b.frequency === 'MONTHLY') return sum + b.amount;
      if (b.frequency === 'WEEKLY') return sum + b.amount * 4.33;
      if (b.frequency === 'QUARTERLY') return sum + b.amount / 3;
      if (b.frequency === 'ANNUALLY') return sum + b.amount / 12;
      return sum + b.amount;
    }, 0);
  });

  billForm = this.fb.group({
    name: ['', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    dueDay: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
    frequency: ['MONTHLY' as BillFrequency, Validators.required],
    isAutoPay: [false]
  });

  ngOnInit(): void {
    this.loadBills();
  }

  loadBills(): void {
    this.loading.set(true);
    this.billService.getBills().subscribe({
      next: (bills) => {
        this.bills.set(bills);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.editingBill.set(null);
    this.billForm.reset({ name: '', amount: 0, dueDay: 1, frequency: 'MONTHLY', isAutoPay: false });
    this.formError.set(null);
    this.showForm.set(true);
  }

  openEditForm(bill: Bill): void {
    this.editingBill.set(bill);
    this.billForm.patchValue({
      name: bill.name,
      amount: bill.amount,
      dueDay: bill.dueDay,
      frequency: bill.frequency,
      isAutoPay: bill.isAutoPay
    });
    this.formError.set(null);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingBill.set(null);
  }

  submitForm(): void {
    if (this.billForm.invalid) {
      this.billForm.markAllAsTouched();
      return;
    }
    this.formLoading.set(true);
    this.formError.set(null);
    const val = this.billForm.value;
    const req: BillRequest = {
      name: val.name!,
      amount: val.amount!,
      dueDay: val.dueDay!,
      frequency: val.frequency! as BillFrequency,
      isAutoPay: val.isAutoPay ?? false
    };
    const obs = this.editingBill()
      ? this.billService.updateBill(this.editingBill()!.id, req)
      : this.billService.createBill(req);

    obs.subscribe({
      next: () => {
        this.formLoading.set(false);
        this.closeForm();
        this.loadBills();
      },
      error: (err) => {
        this.formLoading.set(false);
        this.formError.set(err.error?.message || 'Failed to save bill.');
      }
    });
  }

  markPaid(bill: Bill): void {
    this.billService.markBillPaid(bill.id).subscribe({
      next: () => this.loadBills(),
      error: () => {}
    });
  }

  confirmDelete(bill: Bill): void {
    this.deletingBill.set(bill);
  }

  deleteBill(): void {
    const b = this.deletingBill();
    if (!b) return;
    this.billService.deleteBill(b.id).subscribe({
      next: () => {
        this.deletingBill.set(null);
        this.loadBills();
      },
      error: () => this.deletingBill.set(null)
    });
  }

  isBillPaidThisCycle(bill: Bill): boolean {
    if (!bill.lastPaidDate) return false;
    const lastPaid = new Date(bill.lastPaidDate);
    const nextDue = new Date(bill.nextDueDate);
    const today = new Date();
    // Bill is paid for this cycle if the last payment date is on or after the
    // most recent due date (i.e., nextDueDate - one billing period), approximated
    // by checking that lastPaidDate is not before today minus 31 days and
    // lastPaidDate >= nextDueDate minus the billing period.
    // Simpler: paid if lastPaidDate is this month/cycle (after today minus 31 days).
    const oneMonthAgo = new Date(today);
    oneMonthAgo.setDate(oneMonthAgo.getDate() - 31);
    return lastPaid >= oneMonthAgo && lastPaid <= today;
  }

  getBillStatusInfo(bill: Bill): { label: string; cssClass: string } {
    if (this.isBillPaidThisCycle(bill)) return { label: 'Paid', cssClass: 'badge-green' };
    const today = new Date();
    const due = new Date(bill.nextDueDate);
    const diffDays = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return { label: 'Overdue', cssClass: 'badge-red' };
    if (diffDays <= 7) return { label: `Due in ${diffDays}d`, cssClass: 'badge-yellow' };
    return { label: `Due ${this.formatDate(bill.nextDueDate)}`, cssClass: 'badge-gray' };
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
