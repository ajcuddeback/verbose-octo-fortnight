import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { AuthStore } from './core/store/auth.store';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  template: `
    <div class="app-layout">
      @if (authStore.isAuthenticated()) {
        <app-navbar />
      }
      <main class="main-content" [class.with-nav]="authStore.isAuthenticated()">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      min-height: 100vh;
    }
    .main-content {
      flex: 1;
      overflow-x: hidden;
      min-width: 0;
    }
    .main-content.with-nav {
      padding: 0;
    }
  `]
})
export class AppComponent implements OnInit {
  readonly authStore = inject(AuthStore);
  private authService = inject(AuthService);

  ngOnInit(): void {
    // Always attempt to restore session from the HttpOnly cookie on startup.
    // isAuthenticated() starts false (no user in memory yet), so we must
    // unconditionally call /me — the browser sends the cookie automatically.
    this.authStore.setLoading(true);
    this.authService.getMe().subscribe({
      next: (user) => { this.authStore.setUser(user); this.authStore.setLoading(false); },
      error: () => { this.authStore.clearAuth(); this.authStore.setLoading(false); }
    });
  }
}
