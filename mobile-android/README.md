# Native Android (Kotlin) App

This directory contains the new native Android rewrite for Balance Beacon.

## Scope

- Platform: Android only (Kotlin + Jetpack Compose)
- Backend: Reuses existing web/backend APIs at `/api/v1`
- Auth: JWT access/refresh token flow

## Quick Start

1. Open `mobile-android/` in Android Studio (Hedgehog+).
2. Ensure SDK + JDK are configured.
3. Build and run:
   - `./gradlew :app:assembleDebug`

## Testing

- Run unit tests:
  - `./gradlew :app:testDebugUnitTest`
- Build debug APK:
  - `./gradlew :app:assembleDebug -x lint`

## Config

Default API base URL points to Android emulator host:

- `http://10.0.2.2:3000/api/v1`

Override with Gradle property:

- `BALANCE_BEACON_API_BASE_URL=https://your-host/api/v1`

## Architecture (initial)

- `core/network`: Retrofit/OkHttp + auth interceptor
- `core/storage`: DataStore-backed token persistence
- `core/session`: Session manager and app session state
- `feature/auth`: Login UI + repository
- `navigation`: Root nav graph and destinations

## Next Steps

- Add feature modules for dashboard, transactions, budgets, settings
- Wire API models/repositories endpoint-by-endpoint from existing mobile app
- Add instrumentation and unit test suites
