## 2024-05-23 - Registration DoS Vulnerability
**Vulnerability:** Registration endpoint was rate-limited only by email, allowing an attacker to flood the database with fake accounts using random emails from a single IP.
**Learning:** `checkRateLimitTyped` is generic and stateless, so using it with different keys (email vs IP) for the same endpoint is a simple but effective defense in depth.
**Prevention:** Always consider multiple identifiers (Email, IP, UserID) for rate limiting sensitive endpoints like registration, login, and password reset.
