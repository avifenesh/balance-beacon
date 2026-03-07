import { describe, expect, it, vi, beforeEach } from 'vitest'
import { NextResponse } from 'next/server'

vi.mock('@/lib/rate-limit', () => ({
  getRateLimitHeaders: vi.fn().mockReturnValue({
    'X-RateLimit-Limit': '100',
    'X-RateLimit-Remaining': '99',
  }),
}))

vi.mock('@/lib/subscription', () => ({
  getSubscriptionState: vi.fn(),
}))

import {
  isApiError,
  isApiSuccess,
  errorResponse,
  validationError,
  authError,
  forbiddenError,
  notFoundError,
  rateLimitError,
  serverError,
  successResponse,
  successResponseWithRateLimit,
  subscriptionRequiredError,
  checkSubscription,
  CACHE_STABLE,
  CACHE_DASHBOARD,
} from '@/lib/api-helpers'
import type { ApiResponse, CacheConfig } from '@/lib/api-helpers'
import { getSubscriptionState } from '@/lib/subscription'

describe('api-helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('type guards', () => {
    it('isApiError returns true for error responses', () => {
      const response: ApiResponse<unknown> = { error: 'Something went wrong' }
      expect(isApiError(response)).toBe(true)
    })

    it('isApiError returns false for success responses', () => {
      const response: ApiResponse<unknown> = { success: true, data: { id: '1' } }
      expect(isApiError(response)).toBe(false)
    })

    it('isApiError returns false when error is not a string', () => {
      const response = { error: 123 } as unknown as ApiResponse<unknown>
      expect(isApiError(response)).toBe(false)
    })

    it('isApiSuccess returns true for success responses', () => {
      const response: ApiResponse<{ id: string }> = { success: true, data: { id: '1' } }
      expect(isApiSuccess(response)).toBe(true)
    })

    it('isApiSuccess returns false for error responses', () => {
      const response: ApiResponse<unknown> = { error: 'Something went wrong' }
      expect(isApiSuccess(response)).toBe(false)
    })
  })

  describe('errorResponse', () => {
    it('returns JSON response with error message and status', async () => {
      const res = errorResponse('Not found', 404)
      expect(res).toBeInstanceOf(NextResponse)
      expect(res.status).toBe(404)
      const body = await res.json()
      expect(body).toEqual({ error: 'Not found' })
    })

    it('includes field errors when provided', async () => {
      const fields = { name: ['Name is required'], email: ['Invalid email'] }
      const res = errorResponse('Validation failed', 400, fields)
      expect(res.status).toBe(400)
      const body = await res.json()
      expect(body).toEqual({ error: 'Validation failed', fields })
    })

    it('omits fields property when no field errors', async () => {
      const res = errorResponse('Server error', 500)
      const body = await res.json()
      expect(body).toEqual({ error: 'Server error' })
      expect(body.fields).toBeUndefined()
    })
  })

  describe('validationError', () => {
    it('returns 400 with field errors', async () => {
      const fields = { amount: ['Must be positive'] }
      const res = validationError(fields)
      expect(res.status).toBe(400)
      const body = await res.json()
      expect(body.error).toBe('Validation failed')
      expect(body.fields).toEqual(fields)
    })
  })

  describe('authError', () => {
    it('returns 401 with default message', async () => {
      const res = authError()
      expect(res.status).toBe(401)
      const body = await res.json()
      expect(body.error).toBe('Unauthorized')
    })

    it('returns 401 with custom message', async () => {
      const res = authError('Token expired')
      expect(res.status).toBe(401)
      const body = await res.json()
      expect(body.error).toBe('Token expired')
    })
  })

  describe('forbiddenError', () => {
    it('returns 403 with default message', async () => {
      const res = forbiddenError()
      expect(res.status).toBe(403)
      const body = await res.json()
      expect(body.error).toBe('Forbidden')
    })

    it('returns 403 with custom message', async () => {
      const res = forbiddenError('No access to account')
      expect(res.status).toBe(403)
      const body = await res.json()
      expect(body.error).toBe('No access to account')
    })
  })

  describe('notFoundError', () => {
    it('returns 404 with default message', async () => {
      const res = notFoundError()
      expect(res.status).toBe(404)
      const body = await res.json()
      expect(body.error).toBe('Resource not found')
    })

    it('returns 404 with custom message', async () => {
      const res = notFoundError('Transaction not found')
      expect(res.status).toBe(404)
      const body = await res.json()
      expect(body.error).toBe('Transaction not found')
    })
  })

  describe('rateLimitError', () => {
    it('returns 429 with Retry-After header', async () => {
      const resetAt = new Date(Date.now() + 30_000)
      const res = rateLimitError(resetAt)
      expect(res.status).toBe(429)
      const body = await res.json()
      expect(body.error).toBe('Rate limit exceeded')

      const retryAfter = Number(res.headers.get('Retry-After'))
      expect(retryAfter).toBeGreaterThan(0)
      expect(retryAfter).toBeLessThanOrEqual(30)
    })

    it('returns Retry-After of 0 when reset time is in the past', async () => {
      const resetAt = new Date(Date.now() - 1000)
      const res = rateLimitError(resetAt)
      expect(res.status).toBe(429)

      const retryAfter = Number(res.headers.get('Retry-After'))
      expect(retryAfter).toBe(0)
    })
  })

  describe('serverError', () => {
    it('returns 500 with default message', async () => {
      const res = serverError()
      expect(res.status).toBe(500)
      const body = await res.json()
      expect(body.error).toBe('Internal server error')
    })

    it('returns 500 with custom message', async () => {
      const res = serverError('Database connection failed')
      expect(res.status).toBe(500)
      const body = await res.json()
      expect(body.error).toBe('Database connection failed')
    })
  })

  describe('successResponse', () => {
    it('returns 200 with data by default', async () => {
      const data = { id: '1', name: 'Test' }
      const res = successResponse(data)
      expect(res.status).toBe(200)
      const body = await res.json()
      expect(body).toEqual({ success: true, data })
    })

    it('returns custom status code', async () => {
      const res = successResponse({ created: true }, 201)
      expect(res.status).toBe(201)
      const body = await res.json()
      expect(body.success).toBe(true)
    })

    it('handles null data', async () => {
      const res = successResponse(null)
      const body = await res.json()
      expect(body).toEqual({ success: true, data: null })
    })

    it('handles array data', async () => {
      const data = [{ id: '1' }, { id: '2' }]
      const res = successResponse(data)
      const body = await res.json()
      expect(body.data).toHaveLength(2)
    })

    it('does not set Cache-Control when no cache config', () => {
      const res = successResponse({ id: '1' })
      expect(res.headers.get('Cache-Control')).toBeNull()
    })

    it('sets private Cache-Control with max-age and stale-while-revalidate', () => {
      const cache: CacheConfig = { maxAge: 10, staleWhileRevalidate: 30 }
      const res = successResponse({ id: '1' }, 200, cache)
      expect(res.headers.get('Cache-Control')).toBe('private, max-age=10, stale-while-revalidate=30')
    })

    it('sets public Cache-Control when isPublic is true', () => {
      const cache: CacheConfig = { maxAge: 3600, isPublic: true }
      const res = successResponse({ id: '1' }, 200, cache)
      expect(res.headers.get('Cache-Control')).toBe('public, max-age=3600')
    })

    it('sets Cache-Control without stale-while-revalidate when not provided', () => {
      const cache: CacheConfig = { maxAge: 60 }
      const res = successResponse({ id: '1' }, 200, cache)
      expect(res.headers.get('Cache-Control')).toBe('private, max-age=60')
    })

    it('CACHE_STABLE preset has correct values', () => {
      const res = successResponse({ id: '1' }, 200, CACHE_STABLE)
      expect(res.headers.get('Cache-Control')).toBe('private, max-age=10, stale-while-revalidate=30')
    })

    it('CACHE_DASHBOARD preset has correct values', () => {
      const res = successResponse({ id: '1' }, 200, CACHE_DASHBOARD)
      expect(res.headers.get('Cache-Control')).toBe('private, max-age=30, stale-while-revalidate=60')
    })
  })

  describe('successResponseWithRateLimit', () => {
    it('returns success with rate limit headers', async () => {
      const data = { items: [] }
      const res = successResponseWithRateLimit(data, 'user-1')
      expect(res.status).toBe(200)
      const body = await res.json()
      expect(body.success).toBe(true)
      expect(body.data).toEqual(data)
    })

    it('returns custom status code', async () => {
      const res = successResponseWithRateLimit({ id: '1' }, 'user-1', 'default', 201)
      expect(res.status).toBe(201)
    })
  })

  describe('subscriptionRequiredError', () => {
    it('returns 402 with default message and code', async () => {
      const res = subscriptionRequiredError()
      expect(res.status).toBe(402)
      const body = await res.json()
      expect(body.error).toBe('Active subscription required')
      expect(body.code).toBe('SUBSCRIPTION_REQUIRED')
    })

    it('returns 402 with custom message', async () => {
      const res = subscriptionRequiredError('Upgrade to continue')
      expect(res.status).toBe(402)
      const body = await res.json()
      expect(body.error).toBe('Upgrade to continue')
    })
  })

  describe('checkSubscription', () => {
    it('returns null when subscription is active', async () => {
      vi.mocked(getSubscriptionState).mockResolvedValue({
        canAccessApp: true,
        status: 'ACTIVE',
        isActive: true,
        trialEndsAt: null,
        currentPeriodEnd: new Date(),
        daysRemaining: 30,
      })

      const result = await checkSubscription('user-1')
      expect(result).toBeNull()
    })

    it('returns 402 response when subscription is not active', async () => {
      vi.mocked(getSubscriptionState).mockResolvedValue({
        canAccessApp: false,
        status: 'EXPIRED',
        isActive: false,
        trialEndsAt: null,
        currentPeriodEnd: null,
        daysRemaining: null,
      })

      const result = await checkSubscription('user-1')
      expect(result).not.toBeNull()
      expect(result!.status).toBe(402)
      const body = await result!.json()
      expect(body.code).toBe('SUBSCRIPTION_REQUIRED')
    })
  })
})
