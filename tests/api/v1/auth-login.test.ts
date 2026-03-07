import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'

vi.mock('@/lib/rate-limit', async () => {
  const actual = await vi.importActual('@/lib/rate-limit')
  return {
    ...actual,
    consumeRateLimit: vi.fn().mockResolvedValue({ allowed: true, limit: 3, remaining: 2, resetAt: new Date() }),
  }
})

vi.mock('@/lib/auth-server', () => ({
  verifyCredentials: vi.fn(),
}))

vi.mock('@/lib/prisma', () => ({
  prisma: {
    user: {
      findUnique: vi.fn(),
    },
    refreshToken: {
      create: vi.fn(),
    },
    // rateLimit table not mocked - falls back to in-memory naturally
  },
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: {
    error: vi.fn(),
    info: vi.fn(),
  },
}))

import { POST } from '@/app/api/v1/auth/login/route'
import { verifyCredentials } from '@/lib/auth-server'
import { prisma } from '@/lib/prisma'
import { consumeRateLimit, resetAllRateLimits } from '@/lib/rate-limit'

const mockVerifyCredentials = vi.mocked(verifyCredentials)
const mockUserFindUnique = vi.mocked(prisma.user.findUnique)
const mockRefreshTokenCreate = vi.mocked(prisma.refreshToken.create)
const mockConsumeRateLimit = vi.mocked(consumeRateLimit)

function createRequest(body: unknown) {
  return new NextRequest('http://localhost/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

describe('POST /api/v1/auth/login', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    await resetAllRateLimits()
    process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
    // Default: allow all requests (mock for non-rate-limit tests)
    mockConsumeRateLimit.mockResolvedValue({ allowed: true, limit: 5, remaining: 4, resetAt: new Date() })
  })

  describe('Input Validation', () => {
    it('returns 400 when email is missing', async () => {
      const response = await POST(createRequest({ password: 'pass123' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.email).toBeDefined()
    })

    it('returns 400 when password is missing', async () => {
      const response = await POST(createRequest({ email: 'test@example.com' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.password).toBeDefined()
    })

    it('returns 400 when both fields are missing', async () => {
      const response = await POST(createRequest({}))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.email).toBeDefined()
      expect(data.fields.password).toBeDefined()
    })

    it('returns 500 for malformed JSON', async () => {
      const request = new NextRequest('http://localhost/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await POST(request)
      expect(response.status).toBe(500)
    })
  })

  describe('Authentication', () => {
    it('returns 401 for invalid credentials', async () => {
      mockVerifyCredentials.mockResolvedValue({ valid: false })

      const response = await POST(createRequest({ email: 'test@example.com', password: 'wrong' }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid email or password')
    })

    it('returns 401 for unverified email (generic message)', async () => {
      mockVerifyCredentials.mockResolvedValue({ valid: false, reason: 'email_not_verified' })

      const response = await POST(createRequest({ email: 'test@example.com', password: 'pass' }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid email or password')
    })

    it('passes original email casing to verifyCredentials', async () => {
      mockVerifyCredentials.mockResolvedValue({ valid: false })

      await POST(createRequest({ email: 'USER@Example.COM', password: 'wrong' }))

      expect(mockVerifyCredentials).toHaveBeenCalledWith({
        email: 'USER@Example.COM',
        password: 'wrong',
      })
    })
  })

  describe('Success', () => {
    beforeEach(async () => {
      mockVerifyCredentials.mockResolvedValue({ valid: true, userId: 'user-123' })
      mockUserFindUnique.mockResolvedValue({
        id: 'user-123',
        displayName: 'Test User',
        preferredCurrency: 'USD',
        hasCompletedOnboarding: true,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockRefreshTokenCreate.mockResolvedValue({} as any)
    })

    it('returns 200 with tokens on successful login', async () => {
      const response = await POST(createRequest({ email: 'test@example.com', password: 'correct' }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toMatchObject({
        success: true,
        data: {
          accessToken: expect.any(String),
          refreshToken: expect.any(String),
          expiresIn: 900,
        },
      })
    })

    it('includes user profile in response', async () => {
      const response = await POST(createRequest({ email: 'test@example.com', password: 'correct' }))
      const data = await response.json()

      expect(data.data.user).toMatchObject({
        id: 'user-123',
        displayName: 'Test User',
        preferredCurrency: 'USD',
        hasCompletedOnboarding: true,
      })
    })

    it('creates refresh token in database', async () => {
      await POST(createRequest({ email: 'test@example.com', password: 'correct' }))

      expect(mockRefreshTokenCreate).toHaveBeenCalledWith({
        data: expect.objectContaining({
          userId: 'user-123',
          email: 'test@example.com',
          jti: expect.any(String),
          expiresAt: expect.any(Date),
        }),
      })
    })

    it('returns null user when profile lookup fails', async () => {
      mockUserFindUnique.mockResolvedValue(null)

      const response = await POST(createRequest({ email: 'test@example.com', password: 'correct' }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.data.user).toBeNull()
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when database throws', async () => {
      mockVerifyCredentials.mockResolvedValue({ valid: true, userId: 'user-123' })
      mockUserFindUnique.mockRejectedValue(new Error('DB connection failed'))

      const response = await POST(createRequest({ email: 'test@example.com', password: 'pass' }))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Login failed')
    })
  })
})
