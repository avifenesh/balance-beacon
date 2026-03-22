## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-22 - Timing Attack via Secret String Comparison
**Vulnerability:** Cron endpoints used `!==` for comparing the provided authorization header (`Bearer ${secret}`) against the expected secret, allowing character-by-character timing attacks.
**Learning:** Standard string comparisons terminate early upon the first mismatched character. When comparing secrets (like API keys, webhook signatures, or cron secrets), this response time difference can be measured by attackers to infer the secret.
**Prevention:** Always use constant-time comparisons for secrets. Convert both the expected and provided strings to `Buffer`s, ensure they have the exact same length (`expected.length === provided.length`), and then use `crypto.timingSafeEqual()`.
