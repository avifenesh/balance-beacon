# Mobile Android Native Rewrite Spec

## 1. Goals

- Keep the legacy React Native mobile app removed and continue with native Android app in Kotlin.
- Keep the existing web app and backend APIs unchanged (`Next.js + /api/v1`).
- Preserve feature parity for current mobile user flows:
  - Auth (login/register/reset/verify)
  - Onboarding
  - Dashboard
  - Transactions
  - Budgets
  - Categories
  - Accounts
  - Expense sharing
  - Settings/profile/subscription/paywall
- Maintain existing backend contracts, including JWT + refresh-token flows.

## 2. Non-Goals

- No backend API redesign in this rewrite.
- No web rewrite.
- No iOS target.
- No new product features beyond parity, except migration-enabling technical work.

## 3. Proposed Tech Stack

- Language: Kotlin 1.9.x now (upgrade to Kotlin 2.x together with AGP upgrade)
- UI: Jetpack Compose + Material 3
- Navigation: Navigation Compose
- Networking: Retrofit + OkHttp + Kotlinx Serialization (or Moshi)
- Local persistence:
  - Room (local cache and offline queue)
  - DataStore (user prefs)
  - EncryptedSharedPreferences + Android Keystore (tokens/secrets)
- DI: Hilt
- Async/reactive: Coroutines + Flow
- Logging: Timber
- Testing:
  - Unit: JUnit5 + MockK + Turbine
  - Integration/API: OkHttp MockWebServer
  - UI: Compose UI tests + Espresso

## 4. Module Architecture

Use a feature-first multi-module structure to enable parallel development.

- `:app` - app entrypoint, nav graph composition, DI bootstrap
- `:core:designsystem` - theme, typography, shared Compose components
- `:core:network` - Retrofit services, interceptors, API error mapping
- `:core:auth` - token manager, refresh logic, session guard
- `:core:database` - Room DB, DAOs, migrations
- `:core:model` - domain models + API DTOs
- `:core:common` - constants, result wrappers, utility helpers
- `:feature:auth`
- `:feature:onboarding`
- `:feature:dashboard`
- `:feature:transactions`
- `:feature:budgets`
- `:feature:categories`
- `:feature:accounts`
- `:feature:sharing`
- `:feature:settings`
- `:feature:profile`
- `:feature:subscription`
- `:feature:paywall`

## 5. Package Structure (per feature)

Inside each `:feature:*` module:

- `data/`
  - `remote/` (Retrofit interface + DTO mappers)
  - `local/` (Room entities/dao if needed)
  - `repository/` (single source of truth)
- `domain/`
  - `model/`
  - `usecase/`
- `ui/`
  - `screen/`
  - `components/`
  - `state/` (UiState, intents/events)
  - `vm/` (ViewModel)

## 6. Current Mobile Contract Surface (repo-specific)

Base URL in current app: `API_BASE_URL` defaulting to `http://10.0.2.2:3000/api/v1` (`mobile-android/app/build.gradle.kts`).

Primary endpoints currently used by mobile stores/services:

- Auth/profile
  - `POST /auth/login`
  - `POST /auth/register`
  - `POST /auth/verify-email`
  - `POST /auth/resend-verification`
  - `POST /auth/request-reset`
  - `POST /auth/reset-password`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `GET /users/me`
  - `PATCH /users/me`
  - `PATCH /users/me/currency`
  - `GET /auth/export?format=...`
  - `DELETE /auth/account` (with JSON body)

- Subscription
  - `GET /subscriptions`

- Accounts
  - `GET /accounts`
  - `POST /accounts`
  - `PUT /accounts/{id}`
  - `DELETE /accounts/{id}`
  - `PATCH /accounts/{id}/activate`

- Transactions
  - `GET /transactions?accountId&month&categoryId&type&limit&offset`
  - `POST /transactions`
  - `GET /transactions/{id}`
  - `PUT /transactions/{id}`
  - `DELETE /transactions/{id}`

- Budgets
  - `GET /budgets?accountId&month`
  - `POST /budgets`
  - `DELETE /budgets?accountId&categoryId&monthKey`
  - `POST /budgets/quick` (onboarding)

- Categories
  - `GET /categories?type&includeArchived`
  - `POST /categories`
  - `PUT /categories/{id}`
  - `PATCH /categories/{id}/archive`
  - `POST /categories/bulk` (onboarding)

- Sharing
  - `GET /sharing`
  - `POST /expenses/share`
  - `PATCH /expenses/shares/{id}/paid`
  - `POST /expenses/shares/{id}/decline`
  - `DELETE /expenses/shares/{id}`
  - `POST /expenses/shares/{id}/remind`
  - `GET /users/lookup?email=...`

- Onboarding
  - `POST /seed-data`
  - `POST /onboarding/complete`

## 7. Screen-by-Screen Migration Plan

Source of truth for nav targets: `mobile-android/app/src/main/java/app/balancebeacon/mobileandroid/navigation/AppDestination.kt`.

### Phase A: Foundation + Auth

- `Login`
- `Register`
- `ResetPassword`
- `VerifyEmail`
- Session bootstrap + refresh-token flow

Exit criteria:

- User can cold-start, login, refresh session, and logout without RN app.

### Phase B: Onboarding + Subscription Gate

- `OnboardingWelcome`
- `OnboardingCurrency`
- `OnboardingCategories`
- `OnboardingBudget`
- `OnboardingSampleData`
- `OnboardingComplete`
- `OnboardingBiometric`
- `Paywall`

Exit criteria:

- New user can complete onboarding and reach app home.
- Expired user is blocked by paywall.

### Phase C: Core Finance Flows

- Main tabs:
  - `Dashboard`
  - `Transactions`
  - `Budgets`
  - `Sharing`
  - `Settings`
- Modals/screens:
  - `CreateTransaction`
  - `EditTransaction`
  - `CreateBudget`
  - `ShareExpense`
  - `Accounts`
  - `Categories`
  - `Profile`

Exit criteria:

- CRUD parity for transactions/budgets/categories/accounts.
- Sharing flow parity for create/mark paid/decline/cancel/remind.

### Phase D: Hardening + Cutover

- Error UX parity (network, validation, auth expiry).
- Offline queue parity for transaction creation.
- Analytics/logging parity and crash monitoring setup.
- QA signoff and RN app retirement plan.

## 8. API Integration Plan

### 8.1 Network Layer

- One Retrofit client with:
  - `Authorization: Bearer <accessToken>` interceptor
  - request/response logging (debug only)
  - unified error mapper for backend shape:
    - `error` string
    - optional `fields` map

### 8.2 Auth + Refresh

- Store `accessToken` + `refreshToken` encrypted locally.
- On `401`, try single refresh via `POST /auth/refresh`.
- If refresh fails, clear session and route to auth graph.
- Keep refresh serialized to avoid request storms.

### 8.3 DTO/Model Strategy

- Keep DTOs 1:1 with API contracts.
- Map DTO -> domain model at repository boundary.
- Centralize currency/date normalization (month keys and decimal strings).

### 8.4 Offline Queue (Transactions)

- Keep parity with current behavior in `offlineQueueStore`:
  - queue when offline
  - retry max 3 attempts
  - replace optimistic local item with server item on success
- Implement with Room table `pending_transactions` + WorkManager sync job.

## 9. Security and Storage Plan

- Tokens/credentials:
  - EncryptedSharedPreferences backed by Android Keystore
- Optional biometric unlock:
  - Android BiometricPrompt
  - store refresh token encrypted; require biometric before use
- Local DB encryption:
  - SQLCipher optional if policy requires at-rest encryption for app DB
- Network security:
  - cleartext only in debug for local backend
  - HTTPS required in release builds

## 10. Testing Strategy

### Unit

- ViewModel state transitions
- Repository mapping/error cases
- Token refresh manager and retry guards
- Onboarding and sharing business rules

### Integration

- Retrofit + MockWebServer contract tests for each endpoint group
- Room DAO tests for offline queue and cache behavior

### UI

- Compose tests for critical flows:
  - login/register/reset
  - create/edit/delete transaction
  - create/delete budget
  - share expense + participant actions
  - paywall gating

### Regression Gates

- Must pass:
  - auth flow suite
  - transactions suite
  - budgets suite
  - sharing suite
  - onboarding suite

## 11. Rollout Gates

- Gate 1: Foundation ready
  - project builds
  - auth graph functional
  - refresh flow stable

- Gate 2: Core parity ready
  - dashboard/transactions/budgets/categories/accounts complete

- Gate 3: Sharing + settings parity ready

- Gate 4: Hardening complete
  - crash-free smoke run
  - offline sync validated
  - no P1/P2 defects

- Gate 5: Cutover
  - Android native app marked default mobile client
  - RN mobile freeze then archive

## 12. Risks and Mitigations

- Risk: API drift while rewrite in progress
  - Mitigation: freeze mobile API schema changes or maintain adapter layer + contract tests.

- Risk: auth edge-case regressions (refresh races, logout semantics)
  - Mitigation: dedicated auth integration test suite and serialized refresh manager.

- Risk: loss of offline behavior
  - Mitigation: implement queue early (Phase C start), not at the end.

- Risk: velocity dip from big module split
  - Mitigation: start with minimal modules then split by feature after Gate 1.

## 13. Estimated Phases

Assuming 2-3 parallel engineers and no backend rework:

- Phase A (Foundation + Auth): 1.5 to 2.5 weeks
- Phase B (Onboarding + Subscription): 1 to 1.5 weeks
- Phase C (Core Finance + Sharing + Settings): 3 to 4.5 weeks
- Phase D (Hardening + Cutover): 1 to 1.5 weeks

Total: ~6.5 to 10 weeks.

## 14. Parallel Work Plan (immediate)

- Stream 1: Core platform
  - app skeleton, DI, networking, auth refresh manager, encrypted token store
- Stream 2: Feature migration
  - transactions + budgets + categories modules
- Stream 3: Shared UX and quality
  - design system components, nav graph, test harness, CI checks

This split supports immediate parallel execution without blocking on a single module owner.
