import 'server-only'

import { prisma } from '@/lib/prisma'
import { serverLogger } from '@/lib/server-logger'

/**
 * Rate limiting for API endpoints
 *
 * ## Implementation
 * - Primary: Database-backed (PostgreSQL) for persistence across cold starts and instances
 * - Fallback: In-memory store if database is unavailable
 * - Atomic check+increment via Prisma upsert
 *
 * @see docs/RATE_LIMITING.md for full documentation
 */

export type RateLimitType =
  | 'default'
  | 'login'
  | 'registration'
  | 'password_reset'
  | 'resend_verification'
  | 'account_deletion'
  | 'data_export'
  | 'ai_chat'

interface RateLimitConfig {
  windowMs: number
  maxRequests: number
}

export const RATE_LIMIT_CONFIGS: Record<RateLimitType, RateLimitConfig> = {
  default: { windowMs: 60 * 1000, maxRequests: 100 }, // 100/min - general API
  login: { windowMs: 60 * 1000, maxRequests: 5 }, // 5/min - brute force protection
  registration: { windowMs: 60 * 1000, maxRequests: 3 }, // 3/min - spam prevention
  password_reset: { windowMs: 60 * 60 * 1000, maxRequests: 3 }, // 3/hour - abuse prevention
  resend_verification: { windowMs: 15 * 60 * 1000, maxRequests: 3 }, // 3/15min - spam prevention
  account_deletion: { windowMs: 60 * 60 * 1000, maxRequests: 3 }, // 3/hour - abuse prevention
  data_export: { windowMs: 60 * 60 * 1000, maxRequests: 3 }, // 3/hour - GDPR export rate limit
  ai_chat: { windowMs: 60 * 1000, maxRequests: 20 }, // 20/min - AI chat API
}

export interface RateLimitResult {
  allowed: boolean
  limit: number
  remaining: number
  resetAt: Date
}

// =============================================================================
// In-memory fallback store (used when DB is unavailable or in tests)
// =============================================================================

interface RateLimitEntry {
  count: number
  resetTime: Date
}

const rateLimitStore = new Map<string, RateLimitEntry>()

function buildKey(identifier: string, type: RateLimitType): string {
  return `${type}:${identifier}`
}

function checkInMemory(identifier: string, type: RateLimitType): RateLimitResult {
  const config = RATE_LIMIT_CONFIGS[type]
  const key = buildKey(identifier, type)
  const now = new Date()
  const entry = rateLimitStore.get(key)

  if (!entry || now >= entry.resetTime) {
    const resetAt = new Date(now.getTime() + config.windowMs)
    rateLimitStore.set(key, { count: 0, resetTime: resetAt })
    return { allowed: true, limit: config.maxRequests, remaining: config.maxRequests, resetAt }
  }

  const allowed = entry.count < config.maxRequests
  const remaining = Math.max(0, config.maxRequests - entry.count)
  return { allowed, limit: config.maxRequests, remaining, resetAt: entry.resetTime }
}

function incrementInMemory(identifier: string, type: RateLimitType): void {
  const key = buildKey(identifier, type)
  const entry = rateLimitStore.get(key)
  if (entry) {
    entry.count++
  }
}

async function consumeInMemory(identifier: string, type: RateLimitType): Promise<RateLimitResult> {
  const config = RATE_LIMIT_CONFIGS[type]
  const key = buildKey(identifier, type)
  const now = new Date()
  const entry = rateLimitStore.get(key)

  if (!entry || now >= entry.resetTime) {
    const resetAt = new Date(now.getTime() + config.windowMs)
    rateLimitStore.set(key, { count: 1, resetTime: resetAt })
    return { allowed: true, limit: config.maxRequests, remaining: config.maxRequests - 1, resetAt }
  }

  entry.count++
  const allowed = entry.count <= config.maxRequests
  const remaining = Math.max(0, config.maxRequests - entry.count)
  return { allowed, limit: config.maxRequests, remaining, resetAt: entry.resetTime }
}

// =============================================================================
// Database-backed rate limiting (primary)
// =============================================================================

/**
 * Atomically check and consume one rate limit token.
 * Uses PostgreSQL upsert for cross-instance consistency.
 * Falls back to in-memory if database is unavailable.
 */
async function checkDatabase(identifier: string, type: RateLimitType): Promise<RateLimitResult> {
  const config = RATE_LIMIT_CONFIGS[type]
  const key = buildKey(identifier, type)
  const now = new Date()

  // Upsert: create new window or get existing
  const entry = await prisma.rateLimit.upsert({
    where: { key },
    create: {
      key,
      count: 1,
      windowStart: now,
      windowMs: config.windowMs,
    },
    update: {
      // If window expired, reset; otherwise increment
      count: {
        // We'll handle window expiry logic after the upsert
        increment: 1,
      },
    },
  })

  const windowEnd = new Date(entry.windowStart.getTime() + config.windowMs)

  // Window expired — reset it
  if (now >= windowEnd) {
    const newEntry = await prisma.rateLimit.update({
      where: { key },
      data: {
        count: 1,
        windowStart: now,
        windowMs: config.windowMs,
      },
    })
    const newWindowEnd = new Date(newEntry.windowStart.getTime() + config.windowMs)
    return {
      allowed: true,
      limit: config.maxRequests,
      remaining: config.maxRequests - 1,
      resetAt: newWindowEnd,
    }
  }

  // Window still active — check count
  const allowed = entry.count <= config.maxRequests
  const remaining = Math.max(0, config.maxRequests - entry.count)

  return { allowed, limit: config.maxRequests, remaining, resetAt: windowEnd }
}

// =============================================================================
// Public API
// =============================================================================

/**
 * Check and consume one rate limit token for the given identifier and type.
 * Combines the old checkRateLimit + incrementRateLimit into a single atomic call.
 * Uses database for persistence; falls back to in-memory if DB fails.
 */
export async function consumeRateLimit(identifier: string, type: RateLimitType = 'default'): Promise<RateLimitResult> {
  try {
    return await checkDatabase(identifier, type)
  } catch (error) {
    serverLogger.warn('Rate limit DB fallback to in-memory', { key: buildKey(identifier, type) }, error)
    return consumeInMemory(identifier, type)
  }
}

/**
 * @deprecated Use consumeRateLimit() instead. Kept for backward compatibility in tests.
 */
export function checkRateLimitTyped(identifier: string, type: RateLimitType): RateLimitResult {
  return checkInMemory(identifier, type)
}

/**
 * @deprecated Use consumeRateLimit() instead. Kept for backward compatibility in tests.
 */
export function incrementRateLimitTyped(identifier: string, type: RateLimitType): void {
  incrementInMemory(identifier, type)
}

/** @deprecated Use consumeRateLimit() instead */
export function checkRateLimit(userId: string): RateLimitResult {
  return checkInMemory(userId, 'default')
}

/** @deprecated Use consumeRateLimit() instead */
export function incrementRateLimit(userId: string): void {
  incrementInMemory(userId, 'default')
}

/** Get rate limit headers for response */
export function getRateLimitHeaders(userId: string, type: RateLimitType = 'default'): Record<string, string> {
  // Use in-memory for header generation (already consumed by this point)
  const entry = rateLimitStore.get(buildKey(userId, type))
  const config = RATE_LIMIT_CONFIGS[type]

  if (!entry) {
    return {
      'X-RateLimit-Limit': config.maxRequests.toString(),
      'X-RateLimit-Remaining': config.maxRequests.toString(),
      'X-RateLimit-Reset': Math.floor((Date.now() + config.windowMs) / 1000).toString(),
    }
  }

  return {
    'X-RateLimit-Limit': config.maxRequests.toString(),
    'X-RateLimit-Remaining': Math.max(0, config.maxRequests - entry.count).toString(),
    'X-RateLimit-Reset': Math.floor(entry.resetTime.getTime() / 1000).toString(),
  }
}

/** Reset rate limit for a specific identifier (for testing) */
export function resetRateLimitTyped(identifier: string, type: RateLimitType): void {
  rateLimitStore.delete(buildKey(identifier, type))
}

/** Reset rate limit for a specific user (for testing) */
export function resetRateLimit(userId: string): void {
  resetRateLimitTyped(userId, 'default')
}

/** Reset all rate limits (for testing) - clears both in-memory and database */
export async function resetAllRateLimits(): Promise<void> {
  rateLimitStore.clear()
  try {
    await prisma.rateLimit.deleteMany()
  } catch {
    // DB not available or not needed - in-memory reset is sufficient
  }
}

/**
 * Clean up expired rate limit entries from the database.
 * Call from a cron job periodically.
 */
export async function cleanupExpiredRateLimits(): Promise<number> {
  const now = new Date()
  // Delete entries where windowStart + windowMs < now
  // We need raw SQL since Prisma can't do computed column comparisons
  const result = await prisma.$executeRaw`
    DELETE FROM "RateLimit"
    WHERE "windowStart" + ("windowMs" || ' milliseconds')::interval < ${now}
  `
  return result
}

// =============================================================================
// Cron endpoint rate limiting (stays in-memory — low volume, IP-based)
// =============================================================================

const cronRateLimitStore = new Map<string, number>()
const CRON_RATE_LIMIT_WINDOW_MS = 60 * 1000
const CRON_RATE_LIMIT_MAX_ENTRIES = 1000

export function checkCronRateLimit(identifier: string): boolean {
  const now = Date.now()
  const lastRequest = cronRateLimitStore.get(identifier)

  if (cronRateLimitStore.size > CRON_RATE_LIMIT_MAX_ENTRIES) {
    const cutoff = now - 5 * 60 * 1000
    for (const [key, timestamp] of cronRateLimitStore) {
      if (timestamp < cutoff) cronRateLimitStore.delete(key)
    }
  }

  if (lastRequest && now - lastRequest < CRON_RATE_LIMIT_WINDOW_MS) {
    return false
  }

  cronRateLimitStore.set(identifier, now)
  return true
}

export function resetCronRateLimits(): void {
  cronRateLimitStore.clear()
}
