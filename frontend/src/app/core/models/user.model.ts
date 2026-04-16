export type SubscriptionStatus = 'FREE_TRIAL' | 'ACTIVE' | 'CANCELLED' | 'EXPIRED';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  subscriptionStatus: SubscriptionStatus;
  createdAt: string;
}

// AuthResponse no longer contains a token — JWT is set as an HttpOnly cookie by the backend.
// Use the User type directly after login/register by calling GET /api/auth/me.
export interface AuthResponse {
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
