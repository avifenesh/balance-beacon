## 2024-05-24 - Hash Email Verification Tokens
**Vulnerability:** Email verification tokens were being generated and stored in plaintext in the database.
**Learning:** Storing authentication or verification tokens in plaintext in the database is a security risk. If the database is compromised, attackers can use the tokens to verify arbitrary email addresses or bypass security controls.
**Prevention:** Always hash sensitive tokens (like password reset tokens or email verification tokens) using a secure hashing algorithm (e.g., `crypto.createHash('sha256')`) before storing them in the database. When verifying a token provided by a user, hash the incoming token and compare it to the stored hash. Return the unhashed token to the user for use in emails or links.
