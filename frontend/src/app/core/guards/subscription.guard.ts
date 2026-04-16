import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../store/auth.store';

export const subscriptionGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  if (authStore.isAuthenticated()) {
    if (authStore.isSubscribed()) return true;
    return router.createUrlTree(['/subscription']);
  }
  return router.createUrlTree(['/login']);
};
