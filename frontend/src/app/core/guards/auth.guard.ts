import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthStore } from '../store/auth.store';

export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  // If the auth check has already completed, decide immediately.
  if (authStore.initialized()) {
    return authStore.isAuthenticated() ? true : router.createUrlTree(['/login']);
  }

  // Otherwise wait for initialization to complete before allowing navigation.
  return toObservable(authStore.initialized).pipe(
    filter(initialized => initialized),
    take(1),
    map(() => authStore.isAuthenticated() ? true : router.createUrlTree(['/login']))
  );
};
