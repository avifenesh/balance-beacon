import { createHash } from 'crypto'

/**
 * Hash a plaintext token using SHA-256 for secure database storage.
 * Tokens are always sent to users in plaintext (email links) but
 * stored and compared as hashes to prevent DB compromise exposure.
 */
export function hashToken(token: string): string {
  return createHash('sha256').update(token).digest('hex')
}
