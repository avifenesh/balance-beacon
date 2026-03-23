## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.
## 2024-03-05 - Timing Attack Vulnerability in CRON Secret Verification
**Vulnerability:** CRON API endpoints (`/api/cron/subscriptions/route.ts` and `/api/cron/cleanup/route.ts`) verified the `CRON_SECRET` using a standard string inequality operator (`!==`).
**Learning:** Standard string comparisons evaluate character-by-character and return early upon the first mismatch, allowing attackers to deduce the secret's value by measuring response times (timing attack).
**Prevention:** Always use `crypto.timingSafeEqual` with `Buffer` objects for cryptographic secret comparisons, ensuring a constant-time execution path. Always verify buffer lengths are equal before calling `timingSafeEqual` to avoid throwing errors on length mismatches.
