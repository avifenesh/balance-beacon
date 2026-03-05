import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { POST as RefreshExchangeRates } from '@/app/api/v1/exchange-rates/refresh/route'
import { generateAccessToken } from '@/lib/jwt'
import { resetEnvCache } from '@/lib/env-schema'
import { TEST_USER_ID } from './helpers'

vi.mock('@/lib/currency', () => ({
  refreshExchangeRates: vi.fn(),
}))

describe('Exchange Rates Refresh API Route', () => {
  let validToken: string

  beforeEach(() => {
    process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
    resetEnvCache()
    validToken = generateAccessToken(TEST_USER_ID, 'api-test@example.com')
    vi.clearAllMocks()
  })

  it('refreshes exchange rates with valid JWT', async () => {
    const now = new Date('2026-03-05T10:00:00.000Z')
    const { refreshExchangeRates } = await import('@/lib/currency')
    vi.mocked(refreshExchangeRates).mockResolvedValue({
      success: true,
      updatedAt: now,
    })

    const request = new NextRequest('http://localhost/api/v1/exchange-rates/refresh', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${validToken}`,
      },
    })

    const response = await RefreshExchangeRates(request)
    const data = await response.json()

    expect(response.status).toBe(200)
    expect(data.success).toBe(true)
    expect(data.data.updatedAt).toBe(now.toISOString())
  })

  it('returns 401 with missing token', async () => {
    const request = new NextRequest('http://localhost/api/v1/exchange-rates/refresh', {
      method: 'POST',
    })

    const response = await RefreshExchangeRates(request)
    expect(response.status).toBe(401)
  })

  it('returns 500 when refresh fails', async () => {
    const now = new Date('2026-03-05T10:00:00.000Z')
    const { refreshExchangeRates } = await import('@/lib/currency')
    vi.mocked(refreshExchangeRates).mockResolvedValue({
      error: { general: ['Rate provider unavailable'] },
      updatedAt: now,
    })

    const request = new NextRequest('http://localhost/api/v1/exchange-rates/refresh', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${validToken}`,
      },
    })

    const response = await RefreshExchangeRates(request)
    const data = await response.json()

    expect(response.status).toBe(500)
    expect(data.error).toContain('Rate provider unavailable')
  })
})
