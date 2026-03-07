## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-02-13 - Timing Attack on Cron Secret
**Vulnerability:** Cron endpoints used standard string equality (`!==`) to verify the `CRON_SECRET` authorization header.
**Learning:** Standard string equality checks return early as soon as a character mismatch is found, allowing attackers to guess secrets character-by-character by measuring response times.
**Prevention:** Use `crypto.timingSafeEqual` for all secret or token verifications. Ensure the lengths are compared first (and match) before passing Buffers to `timingSafeEqual`.
