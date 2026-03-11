## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.
## 2026-03-11 - Hashing Password Reset Tokens
**Vulnerability:** Password reset tokens were stored in plaintext in the database, allowing an attacker with DB access to reset passwords and hijack accounts.
**Learning:** Raw tokens were stored instead of cryptographic hashes. While random, they act as equivalent to a temporary password, thus should be treated similarly.
**Prevention:** Always use `crypto.createHash('sha256')` to hash randomly generated tokens before database insertion, and hash incoming tokens before database querying.
