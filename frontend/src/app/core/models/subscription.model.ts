import { SubscriptionStatus } from './user.model';

export interface Subscription {
  id: number;
  userId: number;
  status: SubscriptionStatus;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd: boolean;
  stripeCustomerId?: string;
  stripeSubscriptionId?: string;
  createdAt: string;
}

export interface CheckoutSession {
  checkoutUrl: string;
  sessionId: string;
}

export { SubscriptionStatus };
