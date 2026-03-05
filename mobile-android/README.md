# Native Android (Kotlin) App

Native Android is now the only mobile client in this repo.

## Scope

- Platform: Android only (Kotlin + Jetpack Compose)
- Backend: Reuses existing web/backend APIs at `/api/v1` (web/backend unchanged)
- Auth: JWT access/refresh token flow
- iOS and React Native mobile code paths are removed

## Quick Start

1. Open `mobile-android/` in Android Studio (Hedgehog+).
2. Ensure Android SDK + JDK 17 are configured.
3. Run local backend from repo root:
   - `npm run dev`
4. Build or run Android app:
   - `./gradlew :app:assembleDebug`

## Testing

- Android unit tests:
  - `./gradlew :app:testDebugUnitTest`
- Android debug build:
  - `./gradlew :app:assembleDebug -x lint`
- Web E2E tests (Playwright) are local-only and not part of CI:
  - `npm run test:e2e`

## Config

Default Android API base URL (emulator):

- `http://10.0.2.2:3000/api/v1`

Override with Gradle property:

- `BALANCE_BEACON_API_BASE_URL=https://your-host/api/v1`

## Current Architecture

- `core/network`: Retrofit/OkHttp, auth interceptor, shared error mapping
- `core/storage`: DataStore-backed session persistence
- `core/session`: Session manager and auth state
- `core/database`: Room-backed pending transaction queue
- `feature/transactions/data`: WorkManager-powered background pending-transaction sync
- `BalanceBeaconApplication`: startup scheduling for pending transaction sync
- `feature/*`: Auth, onboarding, dashboard, transactions, budgets, categories, accounts, sharing, settings/profile/subscription/paywall, recurring, holdings, assistant
- `navigation`: Root nav graph and feature shell
