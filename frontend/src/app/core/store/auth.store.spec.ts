import { TestBed } from '@angular/core/testing';
import { AuthStore } from './auth.store';
import { User } from '../models/user.model';

/**
 * AuthStore tests — JWT is stored in an HttpOnly cookie managed by the browser.
 * The store holds only the User object loaded via GET /auth/me after the cookie is set.
 * No tokens exist in JavaScript.
 */
describe('AuthStore', () => {
  let store: AuthStore;

  const mockUser: User = {
    id: 1,
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    subscriptionStatus: 'ACTIVE',
    createdAt: '2024-01-01'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(AuthStore);
    store.clearAuth(); // start with clean state
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  describe('initial state', () => {
    it('should start with no user', () => {
      expect(store.user()).toBeNull();
    });

    it('should start unauthenticated', () => {
      expect(store.isAuthenticated()).toBeFalse();
    });
  });

  describe('setUser', () => {
    it('should populate the user signal', () => {
      store.setUser(mockUser);
      expect(store.user()).toEqual(mockUser);
    });

    it('should mark the session as authenticated', () => {
      store.setUser(mockUser);
      expect(store.isAuthenticated()).toBeTrue();
    });

    it('should NOT touch localStorage — the JWT is HttpOnly', () => {
      store.setUser(mockUser);
      expect(localStorage.getItem('token')).toBeNull();
    });
  });

  describe('clearAuth', () => {
    it('should clear the user signal', () => {
      store.setUser(mockUser);
      store.clearAuth();
      expect(store.user()).toBeNull();
    });

    it('should mark the session as unauthenticated', () => {
      store.setUser(mockUser);
      store.clearAuth();
      expect(store.isAuthenticated()).toBeFalse();
    });
  });

  describe('isAuthenticated computed', () => {
    it('should return true when user is set', () => {
      store.setUser(mockUser);
      expect(store.isAuthenticated()).toBeTrue();
    });

    it('should return false after clearAuth', () => {
      store.setUser(mockUser);
      store.clearAuth();
      expect(store.isAuthenticated()).toBeFalse();
    });

    it('should react reactively when user changes', () => {
      expect(store.isAuthenticated()).toBeFalse();
      store.setUser(mockUser);
      expect(store.isAuthenticated()).toBeTrue();
      store.clearAuth();
      expect(store.isAuthenticated()).toBeFalse();
    });
  });

  describe('isSubscribed computed', () => {
    it('should return true when subscription is ACTIVE', () => {
      store.setUser(mockUser); // subscriptionStatus: 'ACTIVE'
      expect(store.isSubscribed()).toBeTrue();
    });

    it('should return false when subscription is FREE_TRIAL', () => {
      store.setUser({ ...mockUser, subscriptionStatus: 'FREE_TRIAL' });
      expect(store.isSubscribed()).toBeFalse();
    });

    it('should return false when subscription is CANCELLED', () => {
      store.setUser({ ...mockUser, subscriptionStatus: 'CANCELLED' });
      expect(store.isSubscribed()).toBeFalse();
    });

    it('should return false when user is null', () => {
      store.clearAuth();
      expect(store.isSubscribed()).toBeFalse();
    });
  });

  describe('fullName computed', () => {
    it('should return full name when user is set', () => {
      store.setUser(mockUser);
      expect(store.fullName()).toBe('John Doe');
    });

    it('should return empty string when no user', () => {
      store.clearAuth();
      expect(store.fullName()).toBe('');
    });
  });
});
