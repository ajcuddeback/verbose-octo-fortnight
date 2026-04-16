import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthStore } from '../../core/store/auth.store';
import { SubscriptionService } from '../../core/services/subscription.service';
import { Subscription } from '../../core/models/subscription.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.css'
})
export class SubscriptionComponent implements OnInit {
  private authStore = inject(AuthStore);
  private subscriptionService = inject(SubscriptionService);

  readonly user = this.authStore.user;
  readonly loading = signal(false);
  readonly checkoutLoading = signal(false);
  readonly cancelLoading = signal(false);
  readonly showCancelConfirm = signal(false);
  readonly subscription = signal<Subscription | null>(null);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly features = [
    { icon: '✓', text: 'Unlimited transaction tracking' },
    { icon: '✓', text: 'Smart budget planning (zero-based budgeting)' },
    { icon: '✓', text: 'Goal tracking & projections' },
    { icon: '✓', text: 'AI-powered spending predictions' },
    { icon: '✓', text: 'Financial Health Score' },
    { icon: '✓', text: 'Spending DNA analysis' },
    { icon: '✓', text: 'Bill tracking & reminders' },
    { icon: '🔜', text: 'Bank account sync (coming soon)' },
  ];

  ngOnInit(): void {
    this.loading.set(true);
    this.subscriptionService.getStatus().subscribe({
      next: (sub) => {
        this.subscription.set(sub);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  subscribe(): void {
    this.checkoutLoading.set(true);
    this.error.set(null);
    // TODO: Redirect to Stripe Checkout URL when integrated
    this.subscriptionService.createCheckout().subscribe({
      next: (session) => {
        // Redirect to Stripe checkout
        window.location.href = session.checkoutUrl;
      },
      error: (err) => {
        this.checkoutLoading.set(false);
        this.error.set('Failed to initiate checkout. Please try again.');
      }
    });
  }

  confirmCancel(): void {
    this.showCancelConfirm.set(true);
  }

  cancelSubscription(): void {
    this.showCancelConfirm.set(false);
    this.cancelLoading.set(true);
    this.subscriptionService.cancelSubscription().subscribe({
      next: (sub) => {
        this.subscription.set(sub);
        this.cancelLoading.set(false);
        this.successMessage.set('Your subscription has been cancelled and will end at the current billing period.');
      },
      error: () => {
        this.cancelLoading.set(false);
        this.error.set('Failed to cancel subscription. Please contact support.');
      }
    });
  }

  get isSubscribed(): boolean {
    return this.user()?.subscriptionStatus === 'ACTIVE';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
  }
}
