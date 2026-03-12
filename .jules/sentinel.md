## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-05 - Auth Header Timing Attack in Cron Endpoints
**Vulnerability:** Simple string comparison (`authHeader !== 'Bearer ' + cronSecret`) in Cron endpoints (`/api/cron/subscriptions` and `/api/cron/cleanup`) allowed for a timing attack to guess the `CRON_SECRET`.
**Learning:** String comparison stops at the first mismatch, allowing an attacker to determine the correct characters one by one by measuring the response time of the API.
**Prevention:** Always use `crypto.timingSafeEqual` for cryptographic token verification. Ensure the buffers being compared are of equal length to avoid `timingSafeEqual` throwing exceptions (e.g., `expectedBuffer.length === providedBuffer.length && crypto.timingSafeEqual(expectedBuffer, providedBuffer)`).
