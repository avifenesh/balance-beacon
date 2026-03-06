import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'
import { generateRefreshToken } from '@/lib/jwt'
import { resetEnvCache } from '@/lib/env-schema'

vi.mock('@/lib/prisma', () => ({
  prisma: {
    refreshToken: {
      deleteMany: vi.fn(),
    },
  },
}))

import { POST } from '@/app/api/v1/auth/logout/route'
import { prisma } from '@/lib/prisma'

const mockDeleteMany = vi.mocked(prisma.refreshToken.deleteMany)

function createRequest(body: unknown) {
  return new NextRequest('http://localhost/api/v1/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

describe('POST /api/v1/auth/logout', () => {
  let validRefreshToken: string
  let validJti: string

  beforeEach(() => {
    vi.clearAllMocks()
    resetAllRateLimits()
    process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
    resetEnvCache()

    const result = generateRefreshToken('user-123', 'test@example.com')
    validRefreshToken = result.token
    validJti = result.jti
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockDeleteMany.mockResolvedValue({ count: 1 } as any)
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

    it('returns 400 for invalid refresh token', async () => {
      const response = await POST(createRequest({ refreshToken: 'invalid-token' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.error).toBe('Invalid refresh token')
    })
  })

  describe('Success', () => {
    it('returns 200 on successful logout', async () => {
      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual({
        success: true,
        data: { message: 'Logged out successfully' },
      })
    })

    it('deletes the refresh token from database by jti', async () => {
      await POST(createRequest({ refreshToken: validRefreshToken }))

      expect(mockDeleteMany).toHaveBeenCalledWith({
        where: { jti: validJti },
      })
    })

    it('succeeds even when token was already deleted (idempotent)', async () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockDeleteMany.mockResolvedValue({ count: 0 } as any)

      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when database throws', async () => {
      mockDeleteMany.mockRejectedValue(new Error('DB connection failed'))

      const response = await POST(createRequest({ refreshToken: validRefreshToken }))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Logout failed')
    })

    it('returns 500 for malformed JSON', async () => {
      const request = new NextRequest('http://localhost/api/v1/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await POST(request)
      expect(response.status).toBe(500)
    })
  })
})
