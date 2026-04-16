import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, RouterLinkActive } from '@angular/router';
import { AuthStore } from '../../../core/store/auth.store';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  private authStore = inject(AuthStore);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly user = this.authStore.user;
  readonly isCollapsed = signal(false);

  readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: '◈' },
    { path: '/transactions', label: 'Transactions', icon: '↕' },
    { path: '/budgets', label: 'Budgets', icon: '⊞' },
    { path: '/goals', label: 'Goals', icon: '◎' },
    { path: '/accounts', label: 'Accounts', icon: '⬡' },
    { path: '/bills', label: 'Bills', icon: '⏰' },
    { path: '/analytics', label: 'Analytics', icon: '⋈' },
    { path: '/settings', label: 'Settings', icon: '⚙' },
  ];

  toggleCollapse(): void {
    this.isCollapsed.update(v => !v);
  }

  logout(): void {
    // Subscribe so the HTTP POST actually executes. The service's tap() clears
    // local auth state; we navigate to /login regardless of server response so
    // the user is always returned to the login page.
    this.authService.logout().subscribe({
      complete: () => this.router.navigate(['/login']),
      error: () => {
        // Even if the server call fails, clear local state and redirect.
        this.authStore.clearAuth();
        this.router.navigate(['/login']);
      }
    });
  }

  get userInitials(): string {
    const u = this.user();
    if (!u) return '?';
    return (u.firstName[0] + u.lastName[0]).toUpperCase();
  }
}
