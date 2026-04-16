import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { BudgetService } from '../../core/services/budget.service';
import { CategoryService } from '../../core/services/category.service';
import { BudgetItem, BudgetSummary } from '../../core/models/budget.model';
import { Category } from '../../core/models/category.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ConfirmDialogComponent, LoadingSpinnerComponent],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.css'
})
export class BudgetsComponent implements OnInit {
  readonly Math = Math;
  private budgetService = inject(BudgetService);
  private categoryService = inject(CategoryService);
  private fb = inject(FormBuilder);

  readonly now = new Date();
  readonly selectedMonth = signal(this.now.getMonth() + 1);
  readonly selectedYear = signal(this.now.getFullYear());
  readonly loading = signal(false);
  readonly budgets = signal<BudgetItem[]>([]);
  readonly summary = signal<BudgetSummary | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly showAddForm = signal(false);
  readonly editingBudget = signal<BudgetItem | null>(null);
  readonly deletingBudget = signal<BudgetItem | null>(null);
  readonly formError = signal<string | null>(null);
  readonly formLoading = signal(false);

  readonly months = [
    'January','February','March','April','May','June',
    'July','August','September','October','November','December'
  ];

  readonly years = Array.from({ length: 5 }, (_, i) => this.now.getFullYear() - 2 + i);

  readonly totalAllocated = computed(() => this.budgets().reduce((s, b) => s + b.allocatedAmount, 0));
  readonly totalSpent = computed(() => this.budgets().reduce((s, b) => s + b.spentAmount, 0));
  readonly overBudgetCount = computed(() => this.budgets().filter(b => b.spentAmount > b.allocatedAmount).length);

  addForm = this.fb.group({
    categoryId: [null as number | null, Validators.required],
    allocatedAmount: [0, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    this.loadData();
    this.categoryService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats.filter(c => c.type === 'EXPENSE' || c.type === 'BOTH')),
      error: () => {}
    });
  }

  loadData(): void {
    this.loading.set(true);
    const month = this.selectedMonth();
    const year = this.selectedYear();

    this.budgetService.getBudgets(month, year).subscribe({
      next: (items) => {
        this.budgets.set(items);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    this.budgetService.getBudgetSummary(month, year).subscribe({
      next: (s) => this.summary.set(s),
      error: () => {}
    });
  }

  onMonthChange(): void { this.loadData(); }
  onYearChange(): void { this.loadData(); }

  openAddForm(): void {
    this.editingBudget.set(null);
    this.addForm.reset({ categoryId: null, allocatedAmount: 0 });
    this.formError.set(null);
    this.showAddForm.set(true);
  }

  openEditForm(budget: BudgetItem): void {
    this.editingBudget.set(budget);
    this.addForm.patchValue({ categoryId: budget.categoryId, allocatedAmount: budget.allocatedAmount });
    this.formError.set(null);
    this.showAddForm.set(true);
  }

  closeForm(): void {
    this.showAddForm.set(false);
    this.editingBudget.set(null);
  }

  submitForm(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    this.formLoading.set(true);
    this.formError.set(null);
    const val = this.addForm.value;
    const req = {
      categoryId: val.categoryId!,
      allocatedAmount: val.allocatedAmount!,
      month: this.selectedMonth(),
      year: this.selectedYear()
    };
    const obs = this.editingBudget()
      ? this.budgetService.updateBudget(this.editingBudget()!.id, req)
      : this.budgetService.createBudget(req);

    obs.subscribe({
      next: () => {
        this.formLoading.set(false);
        this.closeForm();
        this.loadData();
      },
      error: (err) => {
        this.formLoading.set(false);
        this.formError.set(err.error?.message || 'Failed to save budget.');
      }
    });
  }

  confirmDelete(budget: BudgetItem): void {
    this.deletingBudget.set(budget);
  }

  deleteBudget(): void {
    const b = this.deletingBudget();
    if (!b) return;
    this.budgetService.deleteBudget(b.id).subscribe({
      next: () => {
        this.deletingBudget.set(null);
        this.loadData();
      },
      error: () => this.deletingBudget.set(null)
    });
  }

  getProgressClass(budget: BudgetItem): string {
    if (budget.percentage >= 100) return 'progress-red';
    if (budget.percentage >= 80) return 'progress-yellow';
    return 'progress-green';
  }

  getAvailableCategories(): Category[] {
    const usedIds = this.budgets().map(b => b.categoryId);
    const editId = this.editingBudget()?.categoryId;
    return this.categories().filter(c => !usedIds.includes(c.id) || c.id === editId);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }
}
