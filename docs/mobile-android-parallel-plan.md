# Mobile Android Parallel Execution Plan

## Objective

Ship a native Android Kotlin app with parity to current mobile behavior, while keeping web/backend unchanged.

## Tracking Issues

- Stream 1: https://github.com/avifenesh/balance-beacon/issues/378
- Stream 2: https://github.com/avifenesh/balance-beacon/issues/379
- Stream 3: https://github.com/avifenesh/balance-beacon/issues/380
- Stream 4: https://github.com/avifenesh/balance-beacon/issues/381
- Stream 5: https://github.com/avifenesh/balance-beacon/issues/382

## Baseline (already done)

- `mobile-android/` scaffold exists and assembles successfully with:
  - `./gradlew :app:assembleDebug -x lint`
- Auth/session/network/navigation skeleton is in place.
- Legacy React Native app has been removed from `mobile/`.
- E2E CI workflows are removed from `.github/workflows` (local E2E docs remain).

## Parallel Streams

### Stream 1: Core Platform

- Scope
  - Token/session hardening
  - Global error mapping
  - Base design system + app shell
  - Network/auth interceptors and refresh guard
- Files/modules
  - `mobile-android/app/src/main/java/.../core/**`
  - `mobile-android/app/src/main/java/.../ui/**`
  - `mobile-android/app/src/main/java/.../navigation/**`
- Exit criteria
  - Cold start -> login -> refresh -> logout is stable
  - Standard API error surface consumed by feature modules

### Stream 2: Auth + Onboarding + Subscription

- Scope
  - Register/reset/verify flows
  - Onboarding screens and completion flow
  - Subscription/paywall gating
- API groups
  - `/auth/*`, `/users/me`, `/onboarding/*`, `/subscriptions`
- Exit criteria
  - New user completes onboarding and lands on home
  - Expired subscription is blocked by paywall

### Stream 3: Transactions + Budgets

- Scope
  - Transactions list/create/edit/delete
  - Budgets list/create/delete + quick setup
  - Offline queue for transaction create
- API groups
  - `/transactions/*`, `/budgets*`
- Exit criteria
  - Full CRUD parity for transactions/budgets
  - Offline queue retries and sync back to server

### Stream 4: Accounts + Categories + Sharing

- Scope
  - Accounts CRUD/activate
  - Categories CRUD/archive/bulk setup
  - Expense sharing lifecycle (share, remind, paid, decline, delete)
- API groups
  - `/accounts/*`, `/categories*`, `/sharing`, `/expenses/*`, `/users/lookup`
- Exit criteria
  - Existing account/category/sharing behavior matches current mobile flows

### Stream 5: QA + Hardening + Cutover

- Scope
  - Unit + integration + Compose UI tests
  - Stability pass (error states, auth edge cases, offline edge cases)
  - RN mobile freeze + archive plan
- Exit criteria
  - Parity suites pass
  - No P1/P2 defects
  - Native Android is default mobile path

## Dependency Map

- Stream 1 blocks Streams 2-4 for shared infrastructure only.
- Streams 2-4 run in parallel after Stream 1 baseline interfaces are merged.
- Stream 5 starts early (test harness) and continues until cutover.

## Recommended Assignment (3 engineers)

- Engineer A: Stream 1 + support for auth edge cases
- Engineer B: Stream 3
- Engineer C: Stream 4
- Shared rotation: Stream 2 screens + Stream 5 test coverage

## Branch and Merge Rules

- One branch per stream:
  - `codex/android-stream-1-core`
  - `codex/android-stream-2-auth-onboarding`
  - `codex/android-stream-3-transactions-budgets`
  - `codex/android-stream-4-accounts-categories-sharing`
  - `codex/android-stream-5-hardening`
- Merge in small vertical slices (API + repository + screen + tests) to avoid long-running drift.

## First 48 Hours Checklist

1. Lock DTO contracts for all API groups from current mobile services.
2. Finalize shared `Result/Error` model in Stream 1.
3. Build first production flow end-to-end: Login -> Transactions list.
4. Add CI job for `mobile-android` assemble + unit tests.
5. Freeze React Native mobile feature development (bugfix-only mode).
