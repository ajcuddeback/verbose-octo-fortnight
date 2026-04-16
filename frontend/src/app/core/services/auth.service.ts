import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { LoginRequest, RegisterRequest, User } from '../models/user.model';
import { AuthStore } from '../store/auth.store';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private authStore = inject(AuthStore);

  /**
   * Sends credentials to the server. The backend sets an HttpOnly JWT cookie
   * via the Set-Cookie response header — JavaScript never touches the token.
   * After the cookie is set, fetches the user profile to populate the store.
   */
  login(email: string, password: string): Observable<User> {
    return this.http
      .post<void>(`${API_BASE}/auth/login`, { email, password } as LoginRequest)
      .pipe(switchMap(() => this.getMe()));
  }

  /**
   * Registers a new account. Same cookie-based flow as login.
   */
  register(data: RegisterRequest): Observable<User> {
    return this.http
      .post<void>(`${API_BASE}/auth/register`, data)
      .pipe(switchMap(() => this.getMe()));
  }

  /**
   * Fetches the currently authenticated user from the server.
   * The browser automatically attaches the HttpOnly cookie.
   * Returns 401 if no valid session exists.
   */
  getMe(): Observable<User> {
    return this.http.get<User>(`${API_BASE}/auth/me`);
  }

  /**
   * Asks the server to clear the HttpOnly cookie, then wipes local auth state.
   */
  logout(): Observable<void> {
    return this.http
      .post<void>(`${API_BASE}/auth/logout`, {})
      .pipe(tap(() => this.authStore.clearAuth()));
  }

  /** Convenience helper — prefer reading authStore.isAuthenticated() directly in templates. */
  isLoggedIn(): boolean {
    return this.authStore.isAuthenticated();
  }
}
