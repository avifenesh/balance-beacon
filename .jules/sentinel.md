## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.
## 2024-05-24 - [Hash Verification Tokens]
**Vulnerability:** Email verification tokens were stored in plaintext in the database.
**Learning:** This exposes them if the database is compromised, allowing an attacker to verify accounts they control or impersonate users.
**Prevention:** Always hash sensitive tokens (like password reset and email verification tokens) using SHA-256 (`crypto.createHash('sha256')`) before storing them in the database, and hash incoming tokens similarly before querying the database.
