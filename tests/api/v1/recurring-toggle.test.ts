import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

vi.mock('@/lib/api-auth', () => ({
  requireJwtAuth: vi.fn(),
}))

vi.mock('@/lib/services/recurring-service', () => ({
  getRecurringTemplateById: vi.fn(),
  toggleRecurringTemplate: vi.fn(),
}))

vi.mock('@/lib/subscription', () => ({
  getSubscriptionState: vi.fn().mockResolvedValue({ canAccessApp: true }),
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: { error: vi.fn(), info: vi.fn() },
}))

import { PATCH } from '@/app/api/v1/recurring/[id]/toggle/route'
import { requireJwtAuth } from '@/lib/api-auth'
import { getRecurringTemplateById, toggleRecurringTemplate } from '@/lib/services/recurring-service'

const mockRequireJwtAuth = vi.mocked(requireJwtAuth)
const mockGetById = vi.mocked(getRecurringTemplateById)
const mockToggle = vi.mocked(toggleRecurringTemplate)

const mockUser = { userId: 'user-123', email: 'test@example.com' }
const templateId = 'tmpl-1'

function createRequest(body: unknown) {
  return new NextRequest(`http://localhost/api/v1/recurring/${templateId}/toggle`, {
    method: 'PATCH',
    headers: {
      Authorization: 'Bearer valid-token',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })
}

describe('PATCH /api/v1/recurring/[id]/toggle', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockGetById.mockResolvedValue({ id: templateId } as any)
    mockToggle.mockResolvedValue(undefined as never)
  })

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await PATCH(createRequest({ isActive: true }), { params: Promise.resolve({ id: templateId }) })
      expect(response.status).toBe(401)
    })
  })

  describe('Input Validation', () => {
    it('returns 400 when isActive is missing', async () => {
      const response = await PATCH(createRequest({}), { params: Promise.resolve({ id: templateId }) })
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.isActive).toBeDefined()
    })

    it('returns 400 for malformed JSON', async () => {
      const request = new NextRequest(`http://localhost/api/v1/recurring/${templateId}/toggle`, {
        method: 'PATCH',
        headers: { Authorization: 'Bearer valid-token', 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await PATCH(request, { params: Promise.resolve({ id: templateId }) })
      expect(response.status).toBe(400)
    })
  })

  describe('Authorization', () => {
    it('returns 404 when template does not exist', async () => {
      mockGetById.mockResolvedValue(null)

      const response = await PATCH(createRequest({ isActive: true }), { params: Promise.resolve({ id: templateId }) })
      const data = await response.json()

      expect(response.status).toBe(404)
      expect(data.error).toBe('Recurring template not found')
    })
  })

  describe('Success', () => {
    it('returns 200 when activating a template', async () => {
      const response = await PATCH(createRequest({ isActive: true }), { params: Promise.resolve({ id: templateId }) })
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual({
        success: true,
        data: { id: templateId, isActive: true },
      })
    })

    it('returns 200 when deactivating a template', async () => {
      const response = await PATCH(createRequest({ isActive: false }), { params: Promise.resolve({ id: templateId }) })
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.data.isActive).toBe(false)
    })

    it('calls toggleRecurringTemplate with correct parameters', async () => {
      await PATCH(createRequest({ isActive: true }), { params: Promise.resolve({ id: templateId }) })

      expect(mockToggle).toHaveBeenCalledWith({ id: templateId, isActive: true })
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when toggleRecurringTemplate throws', async () => {
      mockToggle.mockRejectedValue(new Error('DB error'))

      const response = await PATCH(createRequest({ isActive: true }), { params: Promise.resolve({ id: templateId }) })
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Unable to toggle recurring template')
    })
  })
})
