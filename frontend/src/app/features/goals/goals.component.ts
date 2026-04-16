import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { GoalService } from '../../core/services/goal.service';
import { Goal } from '../../core/models/goal.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

const GOAL_CATEGORIES = [
  { value: 'EMERGENCY_FUND', label: 'Emergency Fund', icon: '🛡' },
  { value: 'VACATION', label: 'Vacation', icon: '✈' },
  { value: 'HOME', label: 'Home', icon: '🏠' },
  { value: 'CAR', label: 'Car', icon: '🚗' },
  { value: 'EDUCATION', label: 'Education', icon: '📚' },
  { value: 'INVESTMENT', label: 'Investment', icon: '📈' },
  { value: 'RETIREMENT', label: 'Retirement', icon: '🏖' },
  { value: 'OTHER', label: 'Other', icon: '⭐' },
];

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ConfirmDialogComponent, LoadingSpinnerComponent],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.css'
})
export class GoalsComponent implements OnInit {
  private goalService = inject(GoalService);
  private fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly goals = signal<Goal[]>([]);
  readonly showForm = signal(false);
  readonly editingGoal = signal<Goal | null>(null);
  readonly deletingGoal = signal<Goal | null>(null);
  readonly contributingGoal = signal<Goal | null>(null);
  readonly contributeAmount = signal('');
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly goalCategories = GOAL_CATEGORIES;

  readonly Math = Math;

  goalForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    targetAmount: [0, [Validators.required, Validators.min(1)]],
    currentAmount: [0, Validators.min(0)],
    targetDate: ['', Validators.required],
    category: ['OTHER', Validators.required]
  });

  ngOnInit(): void {
    this.loadGoals();
  }

  loadGoals(): void {
    this.loading.set(true);
    this.goalService.getGoals().subscribe({
      next: (goals) => {
        this.goals.set(goals);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.editingGoal.set(null);
    this.goalForm.reset({ name: '', description: '', targetAmount: 0, currentAmount: 0, targetDate: '', category: 'OTHER' });
    this.formError.set(null);
    this.showForm.set(true);
  }

  openEditForm(goal: Goal): void {
    this.editingGoal.set(goal);
    this.goalForm.patchValue({
      name: goal.name,
      description: goal.description ?? '',
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      targetDate: goal.targetDate.split('T')[0],
      category: goal.category
    });
    this.formError.set(null);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingGoal.set(null);
  }

  submitForm(): void {
    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      return;
    }
    this.formLoading.set(true);
    this.formError.set(null);
    const val = this.goalForm.value;
    const req = {
      name: val.name!,
      description: val.description || undefined,
      targetAmount: val.targetAmount!,
      currentAmount: val.currentAmount ?? 0,
      targetDate: val.targetDate!,
      category: val.category!
    };
    const obs = this.editingGoal()
      ? this.goalService.updateGoal(this.editingGoal()!.id, req)
      : this.goalService.createGoal(req);
    obs.subscribe({
      next: () => {
        this.formLoading.set(false);
        this.closeForm();
        this.loadGoals();
      },
      error: (err) => {
        this.formLoading.set(false);
        this.formError.set(err.error?.message || 'Failed to save goal.');
      }
    });
  }

  openContribute(goal: Goal): void {
    this.contributingGoal.set(goal);
    this.contributeAmount.set('');
  }

  submitContribution(): void {
    const amount = parseFloat(this.contributeAmount());
    if (isNaN(amount) || amount <= 0) return;
    const goal = this.contributingGoal();
    if (!goal) return;
    this.goalService.contributeToGoal(goal.id, amount).subscribe({
      next: () => {
        this.contributingGoal.set(null);
        this.loadGoals();
      },
      error: () => {}
    });
  }

  confirmDelete(goal: Goal): void {
    this.deletingGoal.set(goal);
  }

  deleteGoal(): void {
    const g = this.deletingGoal();
    if (!g) return;
    this.goalService.deleteGoal(g.id).subscribe({
      next: () => {
        this.deletingGoal.set(null);
        this.loadGoals();
      },
      error: () => this.deletingGoal.set(null)
    });
  }

  getCategoryIcon(category: string): string {
    return GOAL_CATEGORIES.find(c => c.value === category)?.icon ?? '⭐';
  }

  getProgressColor(pct: number): string {
    if (pct >= 100) return 'var(--accent-green)';
    if (pct >= 60) return 'var(--accent-primary)';
    return 'var(--accent-yellow)';
  }

  getCircumference(): number { return 2 * Math.PI * 45; }

  getDashOffset(pct: number): number {
    const c = this.getCircumference();
    return c - (Math.min(pct, 100) / 100) * c;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
