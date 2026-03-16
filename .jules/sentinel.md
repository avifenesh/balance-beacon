## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2026-03-16 - Unhashed Password Reset Tokens
**Vulnerability:** Password reset tokens were stored in plaintext in the database.
**Learning:** Generating tokens with `randomBytes` and storing them as-is allows anyone with DB access to reset passwords.
**Prevention:** Always hash sensitive tokens using `crypto.createHash('sha256')` before storing them, and hash incoming tokens before lookup.
