import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

vi.mock('@/lib/api-auth', () => ({
  requireJwtAuth: vi.fn(),
}))

vi.mock('@/lib/api-auth-helpers', () => ({
  ensureApiAccountOwnership: vi.fn(),
}))

vi.mock('@/lib/services/recurring-service', () => ({
  applyRecurringTemplates: vi.fn(),
}))

vi.mock('@/lib/subscription', () => ({
  getSubscriptionState: vi.fn().mockResolvedValue({ canAccessApp: true }),
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: { error: vi.fn(), info: vi.fn() },
}))

import { POST } from '@/app/api/v1/recurring/apply/route'
import { requireJwtAuth } from '@/lib/api-auth'
import { ensureApiAccountOwnership } from '@/lib/api-auth-helpers'
import { applyRecurringTemplates } from '@/lib/services/recurring-service'

const mockRequireJwtAuth = vi.mocked(requireJwtAuth)
const mockEnsureOwnership = vi.mocked(ensureApiAccountOwnership)
const mockApply = vi.mocked(applyRecurringTemplates)

const mockUser = { userId: 'user-123', email: 'test@example.com' }
const validBody = {
  accountId: 'acc-1',
  monthKey: '2026-01',
}

function createRequest(body: unknown) {
  return new NextRequest('http://localhost/api/v1/recurring/apply', {
    method: 'POST',
    headers: {
      Authorization: 'Bearer valid-token',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })
}

describe('POST /api/v1/recurring/apply', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    mockEnsureOwnership.mockResolvedValue({ allowed: true })
    mockApply.mockResolvedValue({ created: 3 })
  })

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await POST(createRequest(validBody))
      expect(response.status).toBe(401)
    })
  })

  describe('Input Validation', () => {
    it('returns 400 when accountId is missing', async () => {
      const response = await POST(createRequest({ monthKey: '2026-01' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.accountId).toBeDefined()
    })

    it('returns 400 when monthKey is missing', async () => {
      const response = await POST(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.monthKey).toBeDefined()
    })

    it('returns 400 when monthKey is too short', async () => {
      const response = await POST(createRequest({ accountId: 'acc-1', monthKey: '2026' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.monthKey).toBeDefined()
    })

    it('returns 400 for malformed JSON', async () => {
      const request = new NextRequest('http://localhost/api/v1/recurring/apply', {
        method: 'POST',
        headers: { Authorization: 'Bearer valid-token', 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await POST(request)
      expect(response.status).toBe(400)
    })
  })

  describe('Authorization', () => {
    it('returns 403 when user does not own the account', async () => {
      mockEnsureOwnership.mockResolvedValue({ allowed: false })

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(403)
      expect(data.error).toBe('Access denied')
    })
  })

  describe('Success', () => {
    it('returns 200 with apply results', async () => {
      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual({
        success: true,
        data: { created: 3 },
      })
    })

    it('passes templateIds when provided', async () => {
      await POST(createRequest({ ...validBody, templateIds: ['tmpl-1', 'tmpl-2'] }))

      expect(mockApply).toHaveBeenCalledWith({
        monthKey: '2026-01',
        accountId: 'acc-1',
        templateIds: ['tmpl-1', 'tmpl-2'],
      })
    })

    it('calls applyRecurringTemplates without templateIds when not provided', async () => {
      await POST(createRequest(validBody))

      expect(mockApply).toHaveBeenCalledWith({
        monthKey: '2026-01',
        accountId: 'acc-1',
        templateIds: undefined,
      })
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when applyRecurringTemplates throws', async () => {
      mockApply.mockRejectedValue(new Error('DB error'))

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Unable to create recurring transactions')
    })
  })
})
