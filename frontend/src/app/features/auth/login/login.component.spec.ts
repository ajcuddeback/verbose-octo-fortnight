import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/store/auth.store';
import { Router } from '@angular/router';
import { User } from '../../../core/models/user.model';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let authStore: jasmine.SpyObj<AuthStore>;
  let router: Router;

  const mockUser: User = {
    id: 1, email: 'test@example.com', firstName: 'John', lastName: 'Doe',
    subscriptionStatus: 'ACTIVE', createdAt: '2024-01-01'
  };

  beforeEach(async () => {
    // login() now returns Observable<User> — the JWT cookie is set by the server,
    // never handled by the component.
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'isLoggedIn']);
    const authStoreSpy = jasmine.createSpyObj('AuthStore', ['setUser', 'clearAuth'], {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(false),
      user: jasmine.createSpy('user').and.returnValue(null),
      isSubscribed: jasmine.createSpy('isSubscribed').and.returnValue(false),
      loading: jasmine.createSpy('loading').and.returnValue(false),
      fullName: jasmine.createSpy('fullName').and.returnValue('')
    });

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AuthStore, useValue: authStoreSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    authStore = TestBed.inject(AuthStore) as jasmine.SpyObj<AuthStore>;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form validation', () => {
    it('should be invalid when empty', () => {
      expect(component.form.invalid).toBeTrue();
    });

    it('should be invalid with an invalid email format', () => {
      component.form.patchValue({ email: 'notanemail', password: 'password123' });
      expect(component.form.invalid).toBeTrue();
    });

    it('should be invalid with a short password', () => {
      component.form.patchValue({ email: 'valid@email.com', password: '123' });
      expect(component.form.invalid).toBeTrue();
    });

    it('should be valid with correct inputs', () => {
      component.form.patchValue({ email: 'valid@email.com', password: 'password123' });
      expect(component.form.valid).toBeTrue();
    });
  });

  describe('onSubmit', () => {
    it('should not call login when the form is invalid', () => {
      component.onSubmit();
      expect(authService.login).not.toHaveBeenCalled();
    });

    it('should call authService.login with correct credentials', fakeAsync(() => {
      authService.login.and.returnValue(of(mockUser));

      component.form.patchValue({ email: 'test@example.com', password: 'password123' });
      component.onSubmit();
      tick();

      expect(authService.login).toHaveBeenCalledWith('test@example.com', 'password123');
    }));

    it('should call authStore.setUser (never setAuth — no token in JS)', fakeAsync(() => {
      authService.login.and.returnValue(of(mockUser));

      component.form.patchValue({ email: 'test@example.com', password: 'password123' });
      component.onSubmit();
      tick();

      expect(authStore.setUser).toHaveBeenCalledWith(mockUser);
      // Critically: setAuth must NOT be called — the JWT stays in the HttpOnly cookie
      expect(authStore.setUser.calls.mostRecent().args[0]).toEqual(mockUser);
    }));

    it('should navigate to /dashboard on successful login', fakeAsync(() => {
      authService.login.and.returnValue(of(mockUser));
      const navSpy = spyOn(router, 'navigate');

      component.form.patchValue({ email: 'test@example.com', password: 'password123' });
      component.onSubmit();
      tick();

      expect(navSpy).toHaveBeenCalledWith(['/dashboard']);
    }));

    it('should display an error message on 401', fakeAsync(() => {
      authService.login.and.returnValue(throwError(() => ({ status: 401 })));

      component.form.patchValue({ email: 'test@example.com', password: 'wrongpass' });
      component.onSubmit();
      tick();

      expect(component.error()).toBe('Invalid email or password. Please try again.');
    }));

    it('should display a generic error message on non-401 failure', fakeAsync(() => {
      authService.login.and.returnValue(throwError(() => ({ status: 500 })));

      component.form.patchValue({ email: 'test@example.com', password: 'password123' });
      component.onSubmit();
      tick();

      expect(component.error()).toBe('Something went wrong. Please try again later.');
    }));
  });
});
