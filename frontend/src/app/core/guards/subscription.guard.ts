import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthStore } from '../store/auth.store';

export const subscriptionGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  const check = () => {
    if (!authStore.isAuthenticated()) return router.createUrlTree(['/login']);
    if (authStore.isSubscribed()) return true;
    return router.createUrlTree(['/subscription']);
  };

  if (authStore.initialized()) {
    return check();
  }

  return toObservable(authStore.initialized).pipe(
    filter(initialized => initialized),
    take(1),
    map(() => check())
  );
};
