import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { Prisma } from '@prisma/client'
import { resetAllRateLimits } from '@/lib/rate-limit'
import { generateRefreshToken } from '@/lib/jwt'
import { resetEnvCache } from '@/lib/env-schema'

vi.mock('@/lib/prisma', () => {
  const mockTx = {
    refreshToken: {
      update: vi.fn(),
    },
  }
  return {
    prisma: {
      $transaction: vi.fn(async (fn: (tx: typeof mockTx) => Promise<unknown>) => fn(mockTx)),
      _mockTx: mockTx,
    },
  }
})

import { POST } from '@/app/api/v1/auth/refresh/route'
import { prisma } from '@/lib/prisma'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockTx = (prisma as any)._mockTx
const mockRefreshTokenUpdate = vi.mocked(mockTx.refreshToken.update)

function createRequest(body: unknown) {
  return new NextRequest('http://localhost/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

describe('POST /api/v1/auth/refresh', () => {
  let validRefreshToken: string

  beforeEach(() => {
    vi.clearAllMocks()
    resetAllRateLimits()
    process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
    resetEnvCache()

    const result = generateRefreshToken('user-123', 'test@example.com')
    validRefreshToken = result.token

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockRefreshTokenUpdate.mockResolvedValue({} as any)
  })

  describe('Input Validation', () => {
    it('returns 400 when refreshToken is missing', async () => {
      const response = await POST(createRequest({}))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.error).toBe('Refresh token required')
    })

    it('returns 400 when refreshToken is empty string', async () => {
      const response = await POST(createRequest({ refreshToken: '' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.error).toBe('Refresh token required')
    })

    it('returns 401 for invalid refresh token', async () => {
      const response = await POST(createRequest({ refreshToken: 'invalid-token' }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid or expired refresh token')
    })
  })

  describe('Token Rotation', () => {
    it('returns new access and refresh tokens on success', async () => {
      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual({
        success: true,
        data: {
          accessToken: expect.any(String),
          refreshToken: expect.any(String),
          expiresIn: 900,
        },
      })
    })

    it('rotates the jti in the database', async () => {
      await POST(createRequest({ refreshToken: validRefreshToken }))

      expect(mockRefreshTokenUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({ jti: expect.any(String) }),
          data: expect.objectContaining({
            jti: expect.any(String),
            userId: 'user-123',
            email: 'test@example.com',
            expiresAt: expect.any(Date),
          }),
        }),
      )
    })

    it('returns new tokens different from the original', async () => {
      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(data.data.refreshToken).not.toBe(validRefreshToken)
    })
  })

  describe('Revoked Token', () => {
    it('returns 401 when refresh token has been revoked (P2025)', async () => {
      const error = new Prisma.PrismaClientKnownRequestError('Record not found', {
        code: 'P2025',
        clientVersion: '5.0.0',
      })
      mockRefreshTokenUpdate.mockRejectedValue(error)

      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Refresh token has been revoked')
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when database throws unexpected error', async () => {
      mockRefreshTokenUpdate.mockRejectedValue(new Error('DB connection failed'))

      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Token refresh failed')
    })

    it('returns 500 for malformed JSON', async () => {
      const request = new NextRequest('http://localhost/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await POST(request)
      expect(response.status).toBe(500)
    })
  })
})
