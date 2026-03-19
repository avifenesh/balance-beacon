## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-19 - Timing Attack in API Authorization Header Validation
**Vulnerability:** API endpoints (specifically cron jobs) validated Bearer tokens using simple string equality (`authHeader !== \`Bearer \${secret}\``). This allowed timing attacks where an attacker could theoretically guess the secret character by character by measuring the response time, as string comparison fails at the first mismatched character.
**Learning:** Any secret validation, even for internal or machine-to-machine tokens like API keys or CRON_SECRET, must use constant-time comparison to prevent timing leaks.
**Prevention:** Always use `crypto.timingSafeEqual` after converting both strings to `Buffer` objects and verifying their lengths match (since `timingSafeEqual` throws an error for buffers of different lengths). Example:
```javascript
const providedBuffer = Buffer.from(providedStr || '')
const expectedBuffer = Buffer.from(expectedStr)
const isValid = providedBuffer.length === expectedBuffer.length && crypto.timingSafeEqual(providedBuffer, expectedBuffer)
```
