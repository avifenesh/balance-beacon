## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-10 - Unhashed Verification and Reset Tokens
**Vulnerability:** Email verification and password reset tokens were stored as plaintext strings in the database in legacy API routes.
**Learning:** High-entropy authentication tokens were not consistently hashed before database storage across all API routes, exposing users to account takeover risk in the event of a database compromise.
**Prevention:** Ensure tokens are hashed using an algorithm like SHA-256 before storage and validation. Plaintext tokens should only be used in memory to dispatch to the user via email.
