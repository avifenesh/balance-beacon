import { describe, expect, it, vi, beforeEach } from 'vitest'
import { NextRequest, NextResponse } from 'next/server'

vi.mock('@/lib/api-auth', () => ({
  requireJwtAuth: vi.fn(),
}))

vi.mock('@/lib/rate-limit', () => ({
  checkRateLimitTyped: vi.fn().mockReturnValue({ allowed: true }),
  incrementRateLimitTyped: vi.fn(),
}))

vi.mock('@/lib/api-helpers', () => ({
  authError: vi.fn((msg: string) => NextResponse.json({ error: msg }, { status: 401 })),
  rateLimitError: vi.fn((_resetAt: Date) =>
    NextResponse.json({ error: 'Rate limit exceeded' }, { status: 429, headers: { 'Retry-After': '60' } }),
  ),
  serverError: vi.fn((msg: string) => NextResponse.json({ error: msg }, { status: 500 })),
  checkSubscription: vi.fn().mockResolvedValue(null),
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
  },
}))

import { withApiAuth, parseJsonBody } from '@/lib/api-middleware'
import { requireJwtAuth } from '@/lib/api-auth'
import { checkRateLimitTyped } from '@/lib/rate-limit'
import { checkSubscription } from '@/lib/api-helpers'
import { serverLogger } from '@/lib/server-logger'

function createRequest(url = 'http://localhost:3000/api/v1/test', method = 'GET') {
  return new NextRequest(url, { method })
}

describe('withApiAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(requireJwtAuth).mockReturnValue({ userId: 'user-1', email: 'test@example.com' })
    vi.mocked(checkRateLimitTyped).mockReturnValue({ allowed: true, limit: 100, remaining: 99, resetAt: new Date() })
  })

  it('calls handler with authenticated user on success', async () => {
    const handler = vi.fn().mockResolvedValue(NextResponse.json({ success: true }))
    const request = createRequest()

    const res = await withApiAuth(request, handler)
    expect(handler).toHaveBeenCalledWith({ userId: 'user-1', email: 'test@example.com' })
    expect(res.status).toBe(200)
  })

  it('returns 401 when authentication fails', async () => {
    vi.mocked(requireJwtAuth).mockImplementation(() => {
      throw new Error('Invalid token')
    })

    const handler = vi.fn()
    const res = await withApiAuth(createRequest(), handler)

    expect(res.status).toBe(401)
    expect(handler).not.toHaveBeenCalled()
    expect(serverLogger.warn).toHaveBeenCalled()
  })

  it('returns 401 with generic message for non-Error throws', async () => {
    vi.mocked(requireJwtAuth).mockImplementation(() => {
      throw 'string error'
    })

    const res = await withApiAuth(createRequest(), vi.fn())
    expect(res.status).toBe(401)
  })

  it('returns 429 when rate limited', async () => {
    vi.mocked(checkRateLimitTyped).mockReturnValue({
      allowed: false,
      limit: 100,
      remaining: 0,
      resetAt: new Date(Date.now() + 60_000),
    })

    const handler = vi.fn()
    const res = await withApiAuth(createRequest(), handler)

    expect(res.status).toBe(429)
    expect(handler).not.toHaveBeenCalled()
  })

  it('skips rate limiting when skipRateLimit is true', async () => {
    vi.mocked(checkRateLimitTyped).mockReturnValue({
      allowed: false,
      limit: 100,
      remaining: 0,
      resetAt: new Date(),
    })

    const handler = vi.fn().mockResolvedValue(NextResponse.json({ ok: true }))
    const res = await withApiAuth(createRequest(), handler, { skipRateLimit: true })

    expect(res.status).toBe(200)
    expect(handler).toHaveBeenCalled()
    expect(checkRateLimitTyped).not.toHaveBeenCalled()
  })

  it('checks subscription when requireSubscription is true', async () => {
    vi.mocked(checkSubscription).mockResolvedValue(
      NextResponse.json({ error: 'Subscription required' }, { status: 402 }),
    )

    const handler = vi.fn()
    const res = await withApiAuth(createRequest(), handler, { requireSubscription: true })

    expect(res.status).toBe(402)
    expect(handler).not.toHaveBeenCalled()
    expect(checkSubscription).toHaveBeenCalledWith('user-1')
  })

  it('skips subscription check when requireSubscription is false (default)', async () => {
    const handler = vi.fn().mockResolvedValue(NextResponse.json({ ok: true }))
    await withApiAuth(createRequest(), handler)

    expect(checkSubscription).not.toHaveBeenCalled()
    expect(handler).toHaveBeenCalled()
  })

  it('catches unhandled errors from handler and returns 500', async () => {
    const handler = vi.fn().mockRejectedValue(new Error('Database crashed'))
    const res = await withApiAuth(createRequest(), handler)

    expect(res.status).toBe(500)
    expect(serverLogger.error).toHaveBeenCalledWith(
      'Unhandled API error',
      expect.objectContaining({ userId: 'user-1' }),
      expect.any(Error),
    )
  })

  it('passes custom rateLimitType to rate limit check', async () => {
    const handler = vi.fn().mockResolvedValue(NextResponse.json({ ok: true }))
    await withApiAuth(createRequest(), handler, { rateLimitType: 'login' })

    expect(checkRateLimitTyped).toHaveBeenCalledWith('user-1', 'login')
  })
})

describe('parseJsonBody', () => {
  it('parses valid JSON body', async () => {
    const request = new NextRequest('http://localhost:3000/api/test', {
      method: 'POST',
      body: JSON.stringify({ name: 'Test', amount: 100 }),
      headers: { 'Content-Type': 'application/json' },
    })

    const result = await parseJsonBody(request)
    expect(result).toEqual({ name: 'Test', amount: 100 })
  })

  it('returns null for invalid JSON', async () => {
    const request = new NextRequest('http://localhost:3000/api/test', {
      method: 'POST',
      body: 'not json',
      headers: { 'Content-Type': 'application/json' },
    })

    const result = await parseJsonBody(request)
    expect(result).toBeNull()
    expect(serverLogger.warn).toHaveBeenCalled()
  })

  it('returns null for empty body', async () => {
    const request = new NextRequest('http://localhost:3000/api/test', {
      method: 'POST',
    })

    const result = await parseJsonBody(request)
    expect(result).toBeNull()
  })
})
