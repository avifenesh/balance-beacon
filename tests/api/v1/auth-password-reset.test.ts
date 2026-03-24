import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { createHash } from 'crypto'
import { resetAllRateLimits } from '@/lib/rate-limit'

// Deterministic token for testing
const MOCK_PLAINTEXT_TOKEN = 'a'.repeat(64) // 32 bytes as hex = 64 chars
const MOCK_HASHED_TOKEN = createHash('sha256').update(MOCK_PLAINTEXT_TOKEN).digest('hex')

vi.mock('@/lib/rate-limit', async () => {
  const actual = await vi.importActual('@/lib/rate-limit')
  return {
    ...actual,
    consumeRateLimit: vi.fn().mockResolvedValue({ allowed: true, limit: 3, remaining: 2, resetAt: new Date() }),
  }
})

vi.mock('@/lib/prisma', () => ({
  prisma: {
    user: {
      findUnique: vi.fn(),
      update: vi.fn(),
    },
    refreshToken: {
      deleteMany: vi.fn(),
    },
    // rateLimit table not mocked - falls back to in-memory naturally
  },
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: {
    info: vi.fn(),
    error: vi.fn(),
  },
}))

// Mock crypto.randomBytes to return deterministic values for request-reset route
vi.mock('crypto', async (importOriginal) => {
  const actual = await importOriginal<typeof import('crypto')>()
  return {
    ...actual,
    randomBytes: vi.fn().mockReturnValue({
      toString: () => MOCK_PLAINTEXT_TOKEN,
    }),
  }
})

import { POST as requestResetPost } from '@/app/api/v1/auth/request-reset/route'
import { POST as resetPasswordPost } from '@/app/api/v1/auth/reset-password/route'
import { prisma } from '@/lib/prisma'
import { consumeRateLimit } from '@/lib/rate-limit'

const mockConsumeRateLimit = vi.mocked(consumeRateLimit)

describe('Password Reset Flow', () => {
  beforeEach(async () => {
    await resetAllRateLimits()
    vi.clearAllMocks()
    // Default: allow all requests (mock for non-rate-limit tests)
    mockConsumeRateLimit.mockResolvedValue({ allowed: true, limit: 3, remaining: 2, resetAt: new Date() })
  })

  describe('POST /api/v1/auth/request-reset', () => {
    const buildRequest = (body: unknown) =>
      new NextRequest('http://localhost/api/v1/auth/request-reset', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })

    describe('success cases', () => {
      it('generates reset token for existing user', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: null,
          passwordResetExpires: null,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.user.update).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: 'reset-token',
          passwordResetExpires: new Date(),
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })

        const response = await requestResetPost(buildRequest({ email: 'test@example.com' }))

        const data = await response.json()
        expect(response.status).toBe(200)
        expect(data.success).toBe(true)
        expect(data.data.message).toContain('If an account exists')
        expect(prisma.user.update).toHaveBeenCalled()
      })

      it('stores SHA-256 hash of token (not plaintext)', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: null,
          passwordResetExpires: null,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.user.update).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: MOCK_HASHED_TOKEN,
          passwordResetExpires: new Date(),
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })

        await requestResetPost(buildRequest({ email: 'test@example.com' }))

        // Verify prisma.user.update was called with the SHA-256 hash of the token
        expect(prisma.user.update).toHaveBeenCalledWith(
          expect.objectContaining({
            data: expect.objectContaining({
              passwordResetToken: MOCK_HASHED_TOKEN,
            }),
          }),
        )
      })

      it('returns same success message for non-existent email (email enumeration protection)', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce(null)

        const response = await requestResetPost(buildRequest({ email: 'nonexistent@example.com' }))

        const data = await response.json()
        expect(response.status).toBe(200)
        expect(data.success).toBe(true)
        expect(data.data.message).toContain('If an account exists')
        expect(prisma.user.update).not.toHaveBeenCalled()
      })
    })

    describe('validation errors', () => {
      it('returns 400 for invalid email', async () => {
        const response = await requestResetPost(buildRequest({ email: 'not-an-email' }))

        expect(response.status).toBe(400)
        const data = await response.json()
        expect(data.fields?.email).toBeDefined()
      })

      it('returns 400 for missing email', async () => {
        const response = await requestResetPost(buildRequest({}))

        expect(response.status).toBe(400)
      })

      it('returns 400 for malformed JSON', async () => {
        const response = await requestResetPost(
          new NextRequest('http://localhost/api/v1/auth/request-reset', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: 'not json',
          }),
        )

        expect(response.status).toBe(400)
      })
    })

    describe('rate limiting', () => {
      it('returns 429 after 3 attempts for same email within 1 hour', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValue({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: null,
          passwordResetExpires: null,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.user.update).mockResolvedValue({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: 'reset-token',
          passwordResetExpires: new Date(),
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })

        // Mock rate limit to allow first 3, block 4th
        const resetAt = new Date(Date.now() + 3600000)
        mockConsumeRateLimit
          .mockResolvedValueOnce({ allowed: true, limit: 3, remaining: 2, resetAt })
          .mockResolvedValueOnce({ allowed: true, limit: 3, remaining: 1, resetAt })
          .mockResolvedValueOnce({ allowed: true, limit: 3, remaining: 0, resetAt })
          .mockResolvedValueOnce({ allowed: false, limit: 3, remaining: 0, resetAt })

        // First 3 attempts should succeed
        for (let i = 0; i < 3; i++) {
          const response = await requestResetPost(buildRequest({ email: 'ratelimit-reset@example.com' }))
          expect(response.status).not.toBe(429)
        }

        // 4th attempt should be rate limited
        const response = await requestResetPost(buildRequest({ email: 'ratelimit-reset@example.com' }))
        expect(response.status).toBe(429)
        expect(response.headers.get('Retry-After')).toBeTruthy()
      })
    })
  })

  describe('POST /api/v1/auth/reset-password', () => {
    const buildRequest = (body: unknown) =>
      new NextRequest('http://localhost/api/v1/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })

    describe('success cases', () => {
      it('resets password successfully with valid token', async () => {
        const futureDate = new Date(Date.now() + 60 * 60 * 1000)
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'old-hash',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: MOCK_HASHED_TOKEN,
          passwordResetExpires: futureDate,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.user.update).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'new-hash',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: null,
          passwordResetExpires: null,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.refreshToken.deleteMany).mockResolvedValueOnce({ count: 1 })

        const response = await resetPasswordPost(
          buildRequest({
            token: MOCK_PLAINTEXT_TOKEN,
            newPassword: 'NewPassword123',
          }),
        )

        const data = await response.json()
        expect(response.status).toBe(200)
        expect(data.success).toBe(true)
        expect(data.data.message).toContain('reset successfully')
        expect(prisma.user.update).toHaveBeenCalled()
        expect(prisma.refreshToken.deleteMany).toHaveBeenCalledWith({
          where: { userId: 'user-id' },
        })
      })

      it('looks up user by SHA-256 hash of token (not plaintext)', async () => {
        const futureDate = new Date(Date.now() + 60 * 60 * 1000)
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'old-hash',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: MOCK_HASHED_TOKEN,
          passwordResetExpires: futureDate,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.user.update).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'new-hash',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: null,
          passwordResetExpires: null,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })
        vi.mocked(prisma.refreshToken.deleteMany).mockResolvedValueOnce({ count: 1 })

        await resetPasswordPost(
          buildRequest({
            token: MOCK_PLAINTEXT_TOKEN,
            newPassword: 'NewPassword123',
          }),
        )

        // Verify prisma.user.findUnique was called with the SHA-256 hash of the token
        expect(prisma.user.findUnique).toHaveBeenCalledWith(
          expect.objectContaining({
            where: { passwordResetToken: MOCK_HASHED_TOKEN },
          }),
        )
      })

      it('migrates plaintext tokens to hashed storage', async () => {
        const futureDate = new Date(Date.now() + 60 * 60 * 1000)
        // First call (hashed lookup) returns null
        vi.mocked(prisma.user.findUnique)
          .mockResolvedValueOnce(null)
          // Second call (plaintext lookup) returns user (simulating in-flight token)
          .mockResolvedValueOnce({
            id: 'user-id',
            email: 'test@example.com',
            displayName: 'Test User',
            passwordHash: 'old-hash',
            emailVerified: true,
            emailVerificationToken: null,
            emailVerificationExpires: null,
            passwordResetToken: MOCK_PLAINTEXT_TOKEN, // Stored as plaintext
            passwordResetExpires: futureDate,
            preferredCurrency: 'USD',
            hasCompletedOnboarding: false,
            activeAccountId: null,
            deletedAt: null,
            deletedBy: null,
            createdAt: new Date(),
            updatedAt: new Date(),
          })
        vi.mocked(prisma.user.update)
          // Migration update (upgrade to hashed)
          .mockResolvedValueOnce({
            id: 'user-id',
            email: 'test@example.com',
            displayName: 'Test User',
            passwordHash: 'old-hash',
            emailVerified: true,
            emailVerificationToken: null,
            emailVerificationExpires: null,
            passwordResetToken: MOCK_HASHED_TOKEN,
            passwordResetExpires: futureDate,
            preferredCurrency: 'USD',
            hasCompletedOnboarding: false,
            activeAccountId: null,
            deletedAt: null,
            deletedBy: null,
            createdAt: new Date(),
            updatedAt: new Date(),
          })
          // Password update
          .mockResolvedValueOnce({
            id: 'user-id',
            email: 'test@example.com',
            displayName: 'Test User',
            passwordHash: 'new-hash',
            emailVerified: true,
            emailVerificationToken: null,
            emailVerificationExpires: null,
            passwordResetToken: null,
            passwordResetExpires: null,
            preferredCurrency: 'USD',
            hasCompletedOnboarding: false,
            activeAccountId: null,
            deletedAt: null,
            deletedBy: null,
            createdAt: new Date(),
            updatedAt: new Date(),
          })
        vi.mocked(prisma.refreshToken.deleteMany).mockResolvedValueOnce({ count: 1 })

        const response = await resetPasswordPost(
          buildRequest({
            token: MOCK_PLAINTEXT_TOKEN,
            newPassword: 'NewPassword123',
          }),
        )

        const data = await response.json()
        expect(response.status).toBe(200)
        expect(data.success).toBe(true)

        // Verify lookup sequence: first hashed, then plaintext fallback
        expect(prisma.user.findUnique).toHaveBeenCalledTimes(2)
        expect(prisma.user.findUnique).toHaveBeenNthCalledWith(1, {
          where: { passwordResetToken: MOCK_HASHED_TOKEN },
        })
        expect(prisma.user.findUnique).toHaveBeenNthCalledWith(2, {
          where: { passwordResetToken: MOCK_PLAINTEXT_TOKEN },
        })

        // Verify migration: first update upgrades to hashed token
        expect(prisma.user.update).toHaveBeenCalledWith(
          expect.objectContaining({
            where: { id: 'user-id' },
            data: { passwordResetToken: MOCK_HASHED_TOKEN },
          }),
        )
      })
    })

    describe('error cases', () => {
      it('returns 401 for invalid token', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce(null)

        const response = await resetPasswordPost(
          buildRequest({
            token: 'invalid-token',
            newPassword: 'NewPassword123',
          }),
        )

        expect(response.status).toBe(401)
        const data = await response.json()
        expect(data.error).toContain('Invalid or expired')
      })

      it('returns 401 for expired token', async () => {
        const pastDate = new Date(Date.now() - 60 * 60 * 1000)
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: MOCK_HASHED_TOKEN,
          passwordResetExpires: pastDate,
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })

        const response = await resetPasswordPost(
          buildRequest({
            token: MOCK_PLAINTEXT_TOKEN,
            newPassword: 'NewPassword123',
          }),
        )

        expect(response.status).toBe(401)
        const data = await response.json()
        expect(data.error).toContain('Invalid or expired')
      })

      it('returns 401 for token with null expiry', async () => {
        vi.mocked(prisma.user.findUnique).mockResolvedValueOnce({
          id: 'user-id',
          email: 'test@example.com',
          displayName: 'Test User',
          passwordHash: 'hashed',
          emailVerified: true,
          emailVerificationToken: null,
          emailVerificationExpires: null,
          passwordResetToken: MOCK_HASHED_TOKEN,
          passwordResetExpires: null, // null expiry
          preferredCurrency: 'USD',
          hasCompletedOnboarding: false,
          activeAccountId: null,
          deletedAt: null,
          deletedBy: null,
          createdAt: new Date(),
          updatedAt: new Date(),
        })

        const response = await resetPasswordPost(
          buildRequest({
            token: MOCK_PLAINTEXT_TOKEN,
            newPassword: 'NewPassword123',
          }),
        )

        expect(response.status).toBe(401)
        const data = await response.json()
        expect(data.error).toContain('Invalid or expired')
      })
    })

    describe('validation errors', () => {
      it('returns 400 for password too short', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            token: 'valid-token',
            newPassword: 'Pass1',
          }),
        )

        expect(response.status).toBe(400)
        const data = await response.json()
        expect(data.fields?.newPassword).toBeDefined()
      })

      it('returns 400 for password without uppercase', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            token: 'valid-token',
            newPassword: 'password123',
          }),
        )

        expect(response.status).toBe(400)
      })

      it('returns 400 for password without lowercase', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            token: 'valid-token',
            newPassword: 'PASSWORD123',
          }),
        )

        expect(response.status).toBe(400)
      })

      it('returns 400 for password without number', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            token: 'valid-token',
            newPassword: 'PasswordABC',
          }),
        )

        expect(response.status).toBe(400)
      })

      it('returns 400 for missing token', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            newPassword: 'NewPassword123',
          }),
        )

        expect(response.status).toBe(400)
      })

      it('returns 400 for missing newPassword', async () => {
        const response = await resetPasswordPost(
          buildRequest({
            token: 'valid-token',
          }),
        )

        expect(response.status).toBe(400)
      })

      it('returns 400 for malformed JSON', async () => {
        const response = await resetPasswordPost(
          new NextRequest('http://localhost/api/v1/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: 'not json',
          }),
        )

        expect(response.status).toBe(400)
      })
    })
  })
})
