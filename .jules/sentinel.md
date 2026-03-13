## 2024-02-12 - User Enumeration via Timing Attack
**Vulnerability:** Login endpoint exposed user existence via response time difference (bcrypt comparison vs immediate return).
**Learning:** `verifyCredentials` checked user existence before verifying password, allowing ~100ms timing difference.
**Prevention:** Use constant-time comparison logic. Always perform `bcrypt.compare` even if user is not found, using a pre-computed dummy hash.

## 2024-03-13 - Secret Exposure via Timing Attack
**Vulnerability:** Comparing an expected secret string directly with an input utilizing string comparison logic rather than constant-time comparison allows the secret to be guessed one character at a time.
**Learning:** In string comparisons `==` and `!==`, if the characters diverge then the comparison completes and returns. The delay associated with this can be exploited to guess character after character of secrets.
**Prevention:** Convert the expected string and the input into Buffers and then make use of `crypto.timingSafeEqual`. However, it's crucial to first assert that both Buffers are of equal length; `crypto.timingSafeEqual` will explicitly throw an exception if they differ in length.