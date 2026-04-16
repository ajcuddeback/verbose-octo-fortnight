import { Injectable, signal, computed } from '@angular/core';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private _user = signal<User | null>(null);
  private _loading = signal(false);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();

  // Authentication is determined by presence of user data loaded from server
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isSubscribed = computed(() => this._user()?.subscriptionStatus === 'ACTIVE');
  readonly fullName = computed(() => {
    const u = this._user();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  setUser(user: User): void { this._user.set(user); }
  clearAuth(): void { this._user.set(null); }
  setLoading(v: boolean): void { this._loading.set(v); }
}
