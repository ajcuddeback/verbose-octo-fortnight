import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from '../../core/store/auth.store';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit {
  private authStore = inject(AuthStore);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  readonly user = this.authStore.user;
  readonly saveLoading = signal(false);
  readonly saveSuccess = signal(false);
  readonly saveError = signal<string | null>(null);

  profileForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required]
  });

  ngOnInit(): void {
    const u = this.user();
    if (u) {
      this.profileForm.patchValue({ firstName: u.firstName, lastName: u.lastName });
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.saveLoading.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);
    // In a real app, there would be an updateProfile endpoint
    setTimeout(() => {
      this.saveLoading.set(false);
      this.saveSuccess.set(true);
      setTimeout(() => this.saveSuccess.set(false), 3000);
    }, 500);
  }

  getSubscriptionStatusLabel(): string {
    const status = this.user()?.subscriptionStatus;
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'FREE_TRIAL': return 'Free Trial';
      case 'CANCELLED': return 'Cancelled';
      case 'EXPIRED': return 'Expired';
      default: return 'Unknown';
    }
  }

  getSubscriptionBadgeClass(): string {
    const status = this.user()?.subscriptionStatus;
    switch (status) {
      case 'ACTIVE': return 'badge-green';
      case 'FREE_TRIAL': return 'badge-yellow';
      default: return 'badge-red';
    }
  }
}
