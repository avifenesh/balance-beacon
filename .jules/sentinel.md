## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2025-02-14 - Timing Attack on Cron Secret Authorization
**Vulnerability:** Cron endpoints (`/api/cron/subscriptions` and `/api/cron/cleanup`) used string comparison (`!==`) to verify the `CRON_SECRET` authorization header.
**Learning:** Standard string comparisons can exit early on the first mismatched character, allowing attackers to guess the secret character by character by measuring response times.
**Prevention:** Use `crypto.timingSafeEqual` for comparing secrets or tokens. Ensure both the expected and provided values are converted to `Buffer` objects and verify they share the exact same byte length (`Buffer.length`) before calling `timingSafeEqual` to avoid throwing errors on length mismatches.
