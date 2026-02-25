## 2024-02-14 - Auth Rate Limiting Gap
**Vulnerability:** Login and Registration endpoints only rate-limited by user identifier (email), leaving the application vulnerable to credential stuffing and mass account creation from a single IP.
**Learning:** In-memory rate limiting implementation in `src/lib/rate-limit.ts` required adding IP-based types (`login_ip`, `registration_ip`) to support multi-dimensional limits.
**Prevention:** Always implement rate limiting by both user identifier AND client IP for public authentication endpoints. Use `getClientIp` helper to extract IP consistently.
