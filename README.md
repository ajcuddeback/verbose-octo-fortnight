# ClearPath Budget

> A modern, security-first personal budgeting application with predictive analytics and financial intelligence.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-21-red?logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue?logo=typescript)

---

## Overview

ClearPath Budget is a full-stack personal finance application built with a security-first philosophy. Unlike conventional budget apps that store authentication tokens in browser storage (a common XSS vulnerability), ClearPath uses **HttpOnly cookies** for JWT storage — tokens are never accessible from JavaScript.

Beyond security, ClearPath differentiates itself through **Financial Intelligence** features: a composite Financial Health Score (0–100), a Spending DNA personality classifier, linear-regression spending predictions, and a zero-based budgeting mode. The architecture is also designed for a future **bank account sync integration** (Plaid/Yodlee), with placeholder hooks already wired into the Account model and frontend.

The subscription model ($5/month via Stripe) includes responsible data stewardship: cancelled accounts retain their data for 2 years with an automated email warning 30 days before deletion.

---

## Features

### 🔐 Authentication & Security
- **HttpOnly JWT cookies** — tokens never touch JavaScript or localStorage
- CSRF protection via `CookieCsrfTokenRepository` + Angular's automatic `X-XSRF-TOKEN` header
- BCrypt password hashing
- Session restored on app startup via `GET /api/auth/me` (no token refresh needed)
- Auth guard race condition prevented with an `initialized` signal

### 💳 Subscription Model
- **$5/month** plan via Stripe *(integration stubbed — see [Stripe Setup](#stripe-integration))*
- Free trial on signup (`FREE_TRIAL` status)
- All features unlocked after payment confirmation (`ACTIVE` status)
- On cancellation: data retained for **2 years**, then automatically deleted
- **Email notification sent 30 days before deletion** via daily scheduled job

### 📊 Dashboard
- Financial Health Score widget (0–100 composite score with grade A–F)
- Total income / expenses / net savings / savings rate stat cards
- Income vs. expenses bar chart (last 6 months) — Chart.js
- Spending by category donut chart (current month)
- Recent transactions list
- Upcoming bills panel

### 💸 Transactions
- Manual entry with amount, description, date, type (income/expense)
- Category and account association
- Tags and notes
- Filterable by date range and type, searchable by description
- Paginated list view
- Full CRUD with ownership validation

### 📅 Budget Planner
- Monthly budgets per spending category
- Real-time progress bars (spent vs. allocated)
- Over-budget and near-limit alerts
- **Zero-based budgeting panel** — shows unallocated income dollars to assign

### 🎯 Financial Goals
- SVG circular progress indicators
- Target date + projected completion date (based on contribution rate)
- One-click contribution modal
- Goal categories: Emergency Fund, Vacation, Home, Car, Retirement, Education, and more

### 🏦 Accounts
- Multiple account types: Checking, Savings, Credit Card, Investment, Loan
- **Net worth calculation** (assets − liabilities)
- "Bank Account Sync — Coming Soon" placeholder for future Plaid/Yodlee integration

### 🧾 Bills
- Recurring bill tracker (monthly, weekly, quarterly, annually)
- Overdue and due-within-7-days detection with colour coding
- Mark as paid (updates `lastPaidDate`)
- Monthly total calculation across all active bills

### 📈 Analytics
- **Spending Trends** — line chart of income vs. expenses over 12 months
- **Category Breakdown** — donut chart + percentage table for any month
- **Spending Predictions** — linear regression forecast for next 3 months with confidence indicator
- **Financial Health History** — score trend over time
- **Spending DNA** — personality classifier based on top spending categories (e.g. *Experience Seeker*, *Home Nester*, *Essential Minimalist*)
- **Debt Payoff Calculator** — avalanche vs. snowball comparison with payoff timeline chart

### 🗄️ Data Retention
- Daily scheduler at 09:00 checks cancelled accounts
- 30 days before `dataWipeScheduledDate`: sends HTML warning email, sets `dataWipeNotificationSent = true`
- On/after `dataWipeScheduledDate`: deletes all child records in FK-safe order, then deletes the user

---

## Tech Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Backend language | Java | 21 | Application logic |
| Backend framework | Spring Boot | 3.2.5 | REST API, DI, scheduling |
| Security | Spring Security 6 | 6.x | JWT/cookie auth, CSRF |
| Persistence | Spring Data JPA + Hibernate | 3.x | ORM, repositories |
| Database | PostgreSQL | 15+ | Primary data store |
| Email | Spring Mail | 3.x | Transactional email |
| JWT | JJWT | 0.11.5 | Token generation/validation |
| Utilities | Lombok | 1.18.x | Boilerplate reduction |
| Frontend framework | Angular | 21.0.0 | SPA |
| Language | TypeScript | ~5.6 | Type-safe frontend |
| State management | Angular Signals | built-in | Reactive UI state |
| Charts | Chart.js | 4.4.0 | Data visualisations |
| Forms | Angular Reactive Forms | built-in | Validated form inputs |
| Test (backend) | JUnit 5 + Mockito | 5.x / 5.x | Unit tests |
| Test (frontend) | Jasmine + Karma | 5.x | Unit tests |

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| Java | 21 |
| Maven | 3.9 |
| Node.js | 20 |
| npm | 10 |
| PostgreSQL | 15 |

---

## Local Setup

### 1. Clone the repository

```bash
git clone <repo-url>
cd verbose-octo-fortnight
```

### 2. Database setup

```sql
CREATE DATABASE clearpath_budget;
CREATE USER clearpath_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE clearpath_budget TO clearpath_user;
```

### 3. Backend configuration

Open `backend/src/main/resources/application.properties` and fill in every `TODO`:

```properties
# TODO: Set your PostgreSQL username here
spring.datasource.username=clearpath_user

# TODO: Set your PostgreSQL password here
spring.datasource.password=your_password

# TODO: Configure your SMTP email credentials here
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_EMAIL_APP_PASSWORD

# TODO (Stripe): Add your Stripe secret key — see Stripe Integration section
# stripe.api-key=sk_live_...

# TODO (Stripe): Add your Stripe webhook signing secret
# stripe.webhook-secret=whsec_...
```

> The database URL defaults to `jdbc:postgresql://localhost:5432/clearpath_budget`. Change it if your Postgres instance is elsewhere.

### 4. Run the backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

The API starts at **http://localhost:8080**.

On first run, Hibernate auto-creates all tables (`spring.jpa.hibernate.ddl-auto=update`).

### 5. Run the frontend

```bash
cd frontend
npm install
npm start
```

The app opens at **http://localhost:4200**.

### 6. Running tests

```bash
# Backend (uses H2 in-memory DB — no Postgres required)
cd backend && mvn test

# Frontend
cd frontend && npm test
```

---

## Configuration Reference

All keys live in `backend/src/main/resources/application.properties`.

| Key | Required | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | ✅ | `jdbc:postgresql://localhost:5432/clearpath_budget` | PostgreSQL JDBC URL |
| `spring.datasource.username` | ✅ | *(TODO)* | Database username |
| `spring.datasource.password` | ✅ | *(TODO)* | Database password |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | Schema strategy. Use `validate` in production |
| `server.port` | — | `8080` | Backend HTTP port |
| `jwt.secret` | ✅ | *(change this)* | HMAC-SHA256 signing secret (min 32 chars) |
| `jwt.expiration` | — | `86400000` | JWT TTL in milliseconds (24 h) |
| `spring.mail.host` | ✅ | `smtp.gmail.com` | SMTP host |
| `spring.mail.port` | — | `587` | SMTP port |
| `spring.mail.username` | ✅ | *(TODO)* | SMTP username / sender address |
| `spring.mail.password` | ✅ | *(TODO)* | SMTP password or app password |
| `stripe.api-key` | *(Stripe)* | *(TODO)* | Stripe secret key (`sk_live_...` or `sk_test_...`) |
| `stripe.webhook-secret` | *(Stripe)* | *(TODO)* | Stripe webhook signing secret (`whsec_...`) |
| `app.frontend.url` | — | `http://localhost:4200` | Used in email links |

---

## API Endpoints

All protected endpoints require the browser to send the `jwt` HttpOnly cookie (automatic when `withCredentials: true`).

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Create account, sets JWT cookie |
| `POST` | `/api/auth/login` | Public | Authenticate, sets JWT cookie |
| `GET` | `/api/auth/me` | 🍪 Cookie | Returns current user |
| `POST` | `/api/auth/logout` | 🍪 Cookie | Clears JWT cookie |

### Users — `/api/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/profile` | 🍪 Cookie | Get profile |
| `PUT` | `/api/users/profile` | 🍪 Cookie | Update name fields |

### Transactions — `/api/transactions`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/transactions` | 🍪 Cookie | List (query params: `from`, `to`, `type`) |
| `POST` | `/api/transactions` | 🍪 Cookie | Create transaction |
| `GET` | `/api/transactions/{id}` | 🍪 Cookie | Get single |
| `PUT` | `/api/transactions/{id}` | 🍪 Cookie | Update |
| `DELETE` | `/api/transactions/{id}` | 🍪 Cookie | Delete |

### Budgets — `/api/budgets`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/budgets` | 🍪 Cookie | List for month (`?month=&year=`) |
| `GET` | `/api/budgets/summary` | 🍪 Cookie | Summary totals for month |
| `POST` | `/api/budgets` | 🍪 Cookie | Create budget entry |
| `PUT` | `/api/budgets/{id}` | 🍪 Cookie | Update allocated amount |
| `DELETE` | `/api/budgets/{id}` | 🍪 Cookie | Delete |

### Goals — `/api/goals`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/goals` | 🍪 Cookie | List all goals |
| `POST` | `/api/goals` | 🍪 Cookie | Create goal |
| `PUT` | `/api/goals/{id}` | 🍪 Cookie | Update |
| `DELETE` | `/api/goals/{id}` | 🍪 Cookie | Delete |
| `POST` | `/api/goals/{id}/contribute` | 🍪 Cookie | Add contribution amount |

### Accounts — `/api/accounts`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/accounts` | 🍪 Cookie | List active accounts |
| `POST` | `/api/accounts` | 🍪 Cookie | Create account |
| `PUT` | `/api/accounts/{id}` | 🍪 Cookie | Update |
| `DELETE` | `/api/accounts/{id}` | 🍪 Cookie | Soft-delete (`isActive=false`) |

### Bills — `/api/bills`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/bills` | 🍪 Cookie | List active bills |
| `POST` | `/api/bills` | 🍪 Cookie | Create bill |
| `PUT` | `/api/bills/{id}` | 🍪 Cookie | Update |
| `DELETE` | `/api/bills/{id}` | 🍪 Cookie | Delete |
| `POST` | `/api/bills/{id}/paid` | 🍪 Cookie | Mark paid (sets `lastPaidDate`) |

### Subscriptions — `/api/subscriptions`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/subscriptions/status` | 🍪 Cookie | Current subscription status |
| `POST` | `/api/subscriptions/checkout` | 🍪 Cookie | Initiate Stripe checkout *(stubbed)* |
| `POST` | `/api/subscriptions/cancel` | 🍪 Cookie | Cancel subscription |
| `POST` | `/api/subscriptions/webhook` | Public | Stripe webhook receiver *(stubbed)* |

### Analytics — `/api/analytics`

| Method | Path | Auth | Query Params | Description |
|---|---|---|---|---|
| `GET` | `/api/analytics/spending-trends` | 🍪 Cookie | `?months=6` | Monthly income/expense trend |
| `GET` | `/api/analytics/category-breakdown` | 🍪 Cookie | `?month=&year=` | Spending by category |
| `GET` | `/api/analytics/predictions` | 🍪 Cookie | — | Linear regression forecast |
| `GET` | `/api/analytics/spending-dna` | 🍪 Cookie | — | Personality type analysis |
| `GET` | `/api/analytics/financial-health` | 🍪 Cookie | — | Latest health score |
| `GET` | `/api/analytics/financial-health/history` | 🍪 Cookie | `?months=6` | Score over time |

---

## Security Architecture

```
Browser                          Backend
  │                                │
  │── POST /api/auth/login ────────▶│ Authenticates credentials
  │◀─ Set-Cookie: jwt=<token>;     │ Sets HttpOnly + Secure + SameSite=Strict
  │     HttpOnly; Secure;          │ cookie — JS cannot read this
  │     SameSite=Strict            │
  │                                │
  │── GET /api/auth/me ────────────▶│ Browser sends cookie automatically
  │   (cookie sent automatically)  │ Filter reads cookie, validates JWT
  │◀─ 200 { user: {...} } ─────────│ No token in response body
  │                                │
  │── POST /api/transactions ──────▶│ CSRF check:
  │   X-XSRF-TOKEN: <csrf-token>   │   Angular reads XSRF-TOKEN cookie
  │   (cookie sent automatically)  │   Sends as X-XSRF-TOKEN header
  │◀─ 201 Created ─────────────────│   Backend validates match
```

**Key security decisions:**

| Concern | Approach |
|---|---|
| Token storage | HttpOnly cookie — immune to XSS token theft |
| CSRF | `CookieCsrfTokenRepository` (non-HttpOnly XSRF-TOKEN) + Angular automatic header |
| Login/register CSRF | Exempted — no cookie exists yet at this point |
| Stripe webhook CSRF | Exempted — Stripe uses its own HMAC signature verification |
| CORS | `allowCredentials=true`, explicit origin (`http://localhost:4200`), no wildcard |
| Passwords | BCrypt |
| Ownership | Every service method validates `resource.user.id == requestingUser.id` |
| JWT key | HMAC-SHA256 with raw UTF-8 key bytes (no double base64 encoding) |

---

## Stripe Integration

The subscription flow is fully architected but the Stripe SDK calls are stubbed with `TODO` comments. To activate real payments:

**1.** Install the Stripe Java dependency (add to `pom.xml`):
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.x.x</version>
</dependency>
```

**2.** Add credentials to `application.properties`:
```properties
stripe.api-key=sk_test_YOUR_KEY_HERE
stripe.webhook-secret=whsec_YOUR_SECRET_HERE
```

**3.** In `SubscriptionService.java` → `createCheckoutSession()`, replace the stub with:
```java
Stripe.apiKey = stripeApiKey;
SessionCreateParams params = SessionCreateParams.builder()
    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
    .addLineItem(SessionCreateParams.LineItem.builder()
        .setPrice("price_YOUR_STRIPE_PRICE_ID")
        .setQuantity(1L).build())
    .setSuccessUrl(frontendUrl + "/subscription?success=true")
    .setCancelUrl(frontendUrl + "/subscription?cancelled=true")
    .build();
Session session = Session.create(params);
return session.getUrl();
```

**4.** In `SubscriptionService.java` → `handleWebhook()`, implement event handling:
```java
Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
if ("checkout.session.completed".equals(event.getType())) {
    // activate subscription, update user.subscriptionStatus = ACTIVE
}
if ("customer.subscription.deleted".equals(event.getType())) {
    // handle cancellation
}
```

**5.** Set up the webhook in the [Stripe Dashboard](https://dashboard.stripe.com/webhooks) pointing to `https://yourdomain.com/api/subscriptions/webhook`.

---

## Email Configuration

Using Gmail with an [App Password](https://myaccount.google.com/apppasswords):

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-app@gmail.com
spring.mail.password=xxxx-xxxx-xxxx-xxxx   # 16-char App Password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

For other providers (SendGrid, SES, Mailgun), replace `host`, `port`, and credentials accordingly.

---

## Bank Account Integration (Roadmap)

The codebase already includes architectural placeholders for future bank sync:

- **`Account.java`** — contains a `TODO` comment marking where Plaid/Yodlee access tokens should be stored as fields
- **`AccountsComponent`** — renders a "Bank Account Sync — Coming Soon" banner with a visual placeholder

When implementing, the integration would involve:
1. Adding Plaid/Yodlee SDK dependency
2. OAuth flow to obtain `accessToken` and `itemId` per linked account
3. A new `PlaidSyncService` that periodically calls the transactions endpoint and upserts into the `transactions` table
4. Webhook receiver for real-time transaction updates

---

## Subscription & Data Lifecycle

```
  Sign up
     │
     ▼
[FREE_TRIAL] ──── POST /subscriptions/checkout ────▶ [ACTIVE]
                                                          │
                                                  POST /subscriptions/cancel
                                                          │
                                                          ▼
                                                    [CANCELLED]
                                                          │
                                              dataWipeScheduledDate = today + 2 years
                                                          │
                                              ┌───────────┴───────────────────────┐
                                              │                                   │
                                    At -30 days before wipe              At wipe date
                                              │                                   │
                                    Send warning email              Delete all user data
                                    (dataWipeNotificationSent=true)  (transactions, budgets,
                                                                      goals, accounts, bills,
                                                                      then user row)
```

The scheduler runs daily at **09:00** via `@Scheduled(cron = "0 0 9 * * *")` in `DataRetentionScheduler`.

---

## Project Structure

```
verbose-octo-fortnight/
│
├── backend/                          Spring Boot 3.2 API
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/budgetapp/
│       │   │   ├── BudgetAppApplication.java   Entry point (@EnableAsync, @EnableScheduling)
│       │   │   ├── config/                     SecurityConfig, JwtProperties, CORS
│       │   │   ├── controller/                 9 REST controllers
│       │   │   ├── dto/
│       │   │   │   ├── request/                8 validated request DTOs
│       │   │   │   └── response/               14 response DTOs
│       │   │   ├── exception/                  GlobalExceptionHandler + custom exceptions
│       │   │   ├── model/                      9 JPA entities + 4 enums
│       │   │   ├── repository/                 9 Spring Data JPA interfaces
│       │   │   ├── scheduler/                  DataRetentionScheduler (daily @9am)
│       │   │   ├── security/                   JwtTokenProvider, JwtAuthFilter, UserDetailsService
│       │   │   └── service/                    11 services (auth, analytics, health score, email…)
│       │   └── resources/
│       │       └── application.properties      ← fill in TODOs here
│       └── test/
│           └── java/com/budgetapp/
│               ├── controller/                 AuthControllerTest, TransactionControllerTest
│               └── service/                    5 service test classes
│
├── frontend/                         Angular 21 SPA
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/app/
│       ├── app.config.ts             provideRouter, provideHttpClient, XSRF config
│       ├── app.routes.ts             Lazy-loaded routes with authGuard
│       ├── app.component.ts          Session restore on startup
│       ├── core/
│       │   ├── guards/               authGuard, subscriptionGuard (initialized-aware)
│       │   ├── interceptors/         authInterceptor (withCredentials: true)
│       │   ├── models/               7 TypeScript interfaces
│       │   ├── services/             9 HttpClient services
│       │   └── store/                AuthStore, TransactionStore, AnalyticsStore (signals)
│       ├── features/
│       │   ├── auth/                 Login, Register components
│       │   ├── dashboard/            Financial Health Score, charts, recent activity
│       │   ├── transactions/         List + form components
│       │   ├── budgets/              Monthly planner + zero-based budgeting
│       │   ├── goals/                SVG progress cards
│       │   ├── accounts/             Net worth + type-coloured account cards
│       │   ├── bills/                Recurring bill tracker
│       │   ├── analytics/            6-tab analytics suite
│       │   ├── subscription/         $5/month plan + Stripe TODO
│       │   └── settings/             Profile + subscription management
│       └── shared/
│           └── components/           Navbar, FinancialHealthScore, LoadingSpinner, ConfirmDialog
│
└── README.md
```

---

## Known Improvements & Future Work

### Functional Gaps *(implement soon)*

- **Real Stripe payments** — checkout session and webhook handler are stubbed; see [Stripe Integration](#stripe-integration)
- **Email delivery** — SMTP credentials need configuring; currently emails log a warning and no-op
- **Bank account sync** — Plaid/Yodlee OAuth flow; Account model has TODO placeholders
- **Password reset** — no forgot-password / reset-password flow exists
- **Refresh tokens** — current JWT is 24 h with no rotation; add a `refreshToken` endpoint
- **Transaction CSV export** — useful for tax purposes; backend can stream a CSV response
- **Multi-currency support** — all amounts are stored as `BigDecimal` with no currency code

### Technical Debt

- **`AuthService` typed result** — `login()`/`register()` use an `Object[]` return; should be a proper `record AuthResult(String token, AuthResponse response)`
- **Test database** — backend tests use H2; replace with TestContainers for real PostgreSQL integration tests
- **Database migrations** — `ddl-auto=update` is unsafe for production; migrate to Flyway
- **API pagination** — transaction list returns all records; add `page`/`size` query params and a `Page<T>` response wrapper
- **Rate limiting** — no rate limiting on `/api/auth/login`; add Spring Cloud Gateway or Bucket4j
- **Input sanitisation** — add server-side HTML sanitisation on free-text fields (notes, descriptions)
- **API versioning** — no version prefix; add `/api/v1/` before going to production
- **OpenAPI docs** — add `springdoc-openapi` for auto-generated Swagger UI at `/swagger-ui.html`
- **Category seeding** — default categories must be manually inserted; add a `DataInitializer` that seeds system categories on first run

### UX / Frontend

- **Dark / light mode toggle** — CSS custom properties are in place; just needs a theme toggle signal
- **Mobile responsive layout** — navbar and grid layouts need breakpoint improvements for small screens
- **Bulk transaction import** — CSV upload endpoint + frontend file picker
- **Recurring transactions** — auto-generate transactions for recurring bills
- **Receipt photo attachment** — attach images to transactions (S3 / file storage)
- **Year-over-year comparison** — compare spending across years in analytics
- **Real-time bill reminders** — WebSocket push notifications for upcoming bill due dates
- **Budget vs. actual report** — printable monthly summary

### Infrastructure

- **Docker + docker-compose** — containerise backend, frontend (Nginx), and Postgres for one-command local startup
- **CI/CD** — GitHub Actions: run tests on PR, build and push Docker images on merge to `main`
- **Spring profiles** — separate `application-dev.properties` / `application-prod.properties`; use environment variables for secrets in production
- **Secrets management** — replace plain-text credentials in properties with AWS Secrets Manager, HashiCorp Vault, or environment variable injection
- **HikariCP tuning** — configure connection pool size for production load
- **Observability** — add Spring Boot Actuator + Prometheus metrics + Grafana dashboard

---

## Contributing

1. Fork the repository and create a feature branch: `git checkout -b feature/my-feature`
2. Follow the existing code style (Lombok on backend, Angular signals on frontend)
3. Write unit tests for any new service methods or components
4. Ensure `mvn test` and `npm test` pass before opening a PR
5. Open a pull request with a clear description of the change

---

## License

MIT License — see [LICENSE](LICENSE) for details.
