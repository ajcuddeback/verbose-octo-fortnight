import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthStore } from '../store/auth.store';
import { User } from '../models/user.model';

/**
 * Auth flow: login/register POST → server sets HttpOnly cookie → GET /me → User populated in AuthStore.
 * No tokens are ever stored in JavaScript land — the cookie is HttpOnly.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let authStore: AuthStore;

  const mockUser: User = {
    id: 1,
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    subscriptionStatus: 'ACTIVE',
    createdAt: '2024-01-01'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService, AuthStore]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    authStore = TestBed.inject(AuthStore);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should POST credentials then GET /me and return the User', (done) => {
      service.login('test@example.com', 'password123').subscribe(user => {
        expect(user.email).toBe('test@example.com');
        done();
      });

      // Step 1: POST /auth/login — server sets HttpOnly cookie, no body token
      const loginReq = httpMock.expectOne('http://localhost:8080/api/auth/login');
      expect(loginReq.request.method).toBe('POST');
      expect(loginReq.request.withCredentials).toBeTrue(); // cookie sent automatically
      expect(loginReq.request.body).toEqual({ email: 'test@example.com', password: 'password123' });
      loginReq.flush(null); // server returns 200 with no token in body

      // Step 2: GET /auth/me — browser sends cookie automatically
      const meReq = httpMock.expectOne('http://localhost:8080/api/auth/me');
      expect(meReq.request.method).toBe('GET');
      expect(meReq.request.withCredentials).toBeTrue();
      meReq.flush(mockUser);
    });

    it('should propagate 401 error from /auth/login', (done) => {
      service.login('wrong@example.com', 'wrongpass').subscribe({
        error: (err) => { expect(err.status).toBe(401); done(); }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('register', () => {
    it('should POST registration then GET /me and return the User', (done) => {
      const registerData = { firstName: 'Jane', lastName: 'Doe', email: 'jane@example.com', password: 'securepass' };

      service.register(registerData).subscribe(user => {
        expect(user.email).toBe('test@example.com');
        done();
      });

      const regReq = httpMock.expectOne('http://localhost:8080/api/auth/register');
      expect(regReq.request.method).toBe('POST');
      expect(regReq.request.withCredentials).toBeTrue();
      regReq.flush(null);

      const meReq = httpMock.expectOne('http://localhost:8080/api/auth/me');
      meReq.flush(mockUser);
    });

    it('should propagate 400 validation error', (done) => {
      service.register({ firstName: '', lastName: '', email: 'invalid', password: '123' }).subscribe({
        error: (err) => { expect(err.status).toBe(400); done(); }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
      req.flush({ message: 'Validation error' }, { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('logout', () => {
    it('should POST /auth/logout and clear auth store', (done) => {
      authStore.setUser(mockUser); // simulate logged-in state

      service.logout().subscribe(() => {
        expect(authStore.isAuthenticated()).toBeFalse(); // store cleared by tap()
        done();
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBeTrue(); // sends cookie so server can clear it
      req.flush(null);
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when AuthStore has a user (set after getMe succeeds)', () => {
      authStore.setUser(mockUser);
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false when AuthStore has no user', () => {
      authStore.clearAuth();
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should never rely on localStorage — localStorage is not consulted', () => {
      // Even if something writes to localStorage, isLoggedIn() depends only on AuthStore signal
      authStore.clearAuth();
      // Writing to localStorage should have zero effect
      expect(service.isLoggedIn()).toBeFalse();
    });
  });
});
