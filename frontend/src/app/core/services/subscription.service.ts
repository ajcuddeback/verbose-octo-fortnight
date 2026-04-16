import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subscription, CheckoutSession } from '../models/subscription.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private http = inject(HttpClient);

  getStatus(): Observable<Subscription> {
    return this.http.get<Subscription>(`${API_BASE}/subscription/status`);
  }

  createCheckout(): Observable<CheckoutSession> {
    return this.http.post<CheckoutSession>(`${API_BASE}/subscription/checkout`, {});
  }

  cancelSubscription(): Observable<Subscription> {
    return this.http.post<Subscription>(`${API_BASE}/subscription/cancel`, {});
  }
}
