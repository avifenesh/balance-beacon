## 2024-02-12 - User Enumeration via Timing Attack

**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-24 - Plaintext Token Storage

**Vulnerability:** Password reset tokens were generated with `crypto.randomBytes`, sent via email, and directly stored in plaintext in the database via `src/app/api/v1/auth/request-reset/route.ts`. The verification process in `src/app/api/v1/auth/reset-password/route.ts` checked this plaintext stored token directly against the user-provided token.
**Learning:** This is a severe vulnerability. If an attacker gains read access to the database (e.g. via SQL Injection), they can extract any active password reset token and hijack user accounts without access to the user's email inbox.
**Prevention:** Sensitive authentication tokens (like password reset, email verification, or session tokens) should _always_ be treated like passwords. Generate the token, hash it (e.g., using `crypto.createHash('sha256')`), store the hash in the database, and send the plaintext token to the user. When verifying, hash the incoming token and compare it to the stored hash.
