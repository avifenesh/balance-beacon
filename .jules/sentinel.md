## 2024-03-01 - Missing IP-Based Rate Limiting on Auth Endpoints
**Vulnerability:** Authentication endpoints (login, register) lacked IP-based rate limiting, relying solely on email-based rate limiting.
**Learning:** This architectural gap left the application vulnerable to mass distributed attacks or credential stuffing where an attacker could cycle through many different email addresses rapidly from a single IP, bypassing the email-centric rate limit.
**Prevention:** Always implement a multi-layered rate limiting approach for sensitive endpoints. First layer by client IP to block distributed attacks, and a second layer by specific identifier (e.g. email) to protect individual accounts. Use proxy-aware header extraction (`x-forwarded-for`) to accurately identify the client IP.
