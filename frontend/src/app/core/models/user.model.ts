export type SubscriptionStatus = 'FREE_TRIAL' | 'ACTIVE' | 'CANCELLED' | 'EXPIRED';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  subscriptionStatus: SubscriptionStatus;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}
