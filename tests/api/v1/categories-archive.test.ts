import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

vi.mock('@/lib/api-auth', () => ({
  requireJwtAuth: vi.fn(),
}))

vi.mock('@/lib/services/category-service', () => ({
  getCategoryById: vi.fn(),
  archiveCategory: vi.fn(),
}))

vi.mock('@/lib/subscription', () => ({
  getSubscriptionState: vi.fn().mockResolvedValue({ canAccessApp: true }),
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: { error: vi.fn(), info: vi.fn() },
}))

import { PATCH } from '@/app/api/v1/categories/[id]/archive/route'
import { requireJwtAuth } from '@/lib/api-auth'
import { getCategoryById, archiveCategory } from '@/lib/services/category-service'

const mockRequireJwtAuth = vi.mocked(requireJwtAuth)
const mockGetCategoryById = vi.mocked(getCategoryById)
const mockArchiveCategory = vi.mocked(archiveCategory)

const mockUser = { userId: 'user-123', email: 'test@example.com' }
const categoryId = 'cat-1'

function createRequest(body: unknown) {
  return new NextRequest(`http://localhost/api/v1/categories/${categoryId}/archive`, {
    method: 'PATCH',
    headers: {
      Authorization: 'Bearer valid-token',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })
}

describe('PATCH /api/v1/categories/[id]/archive', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockGetCategoryById.mockResolvedValue({ id: categoryId, userId: 'user-123' } as any)
    mockArchiveCategory.mockResolvedValue(undefined as never)
  })

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await PATCH(createRequest({ isArchived: true }), { params: Promise.resolve({ id: categoryId }) })
      expect(response.status).toBe(401)
    })
  })

  describe('Input Validation', () => {
    it('returns 400 when isArchived is missing', async () => {
      const response = await PATCH(createRequest({}), { params: Promise.resolve({ id: categoryId }) })
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.isArchived).toBeDefined()
    })

    it('returns 400 for malformed JSON', async () => {
      const request = new NextRequest(`http://localhost/api/v1/categories/${categoryId}/archive`, {
        method: 'PATCH',
        headers: { Authorization: 'Bearer valid-token', 'Content-Type': 'application/json' },
        body: 'not-json',
      })

      const response = await PATCH(request, { params: Promise.resolve({ id: categoryId }) })
      expect(response.status).toBe(400)
    })
  })

  describe('Authorization', () => {
    it('returns 404 when category does not exist or belongs to another user', async () => {
      mockGetCategoryById.mockResolvedValue(null)

      const response = await PATCH(createRequest({ isArchived: true }), { params: Promise.resolve({ id: categoryId }) })
      const data = await response.json()

      expect(response.status).toBe(404)
      expect(data.error).toBe('Category not found')
    })
  })

  describe('Success', () => {
    it('returns 200 when archiving a category', async () => {
      const response = await PATCH(createRequest({ isArchived: true }), { params: Promise.resolve({ id: categoryId }) })
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual({
        success: true,
        data: { id: categoryId, isArchived: true },
      })
    })

    it('returns 200 when unarchiving a category', async () => {
      const response = await PATCH(createRequest({ isArchived: false }), {
        params: Promise.resolve({ id: categoryId }),
      })
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.data.isArchived).toBe(false)
    })

    it('calls archiveCategory with correct parameters', async () => {
      await PATCH(createRequest({ isArchived: true }), { params: Promise.resolve({ id: categoryId }) })

      expect(mockArchiveCategory).toHaveBeenCalledWith({
        id: categoryId,
        userId: 'user-123',
        isArchived: true,
      })
    })
  })

  describe('Error Handling', () => {
    it('returns 500 when archiveCategory throws', async () => {
      mockArchiveCategory.mockRejectedValue(new Error('DB error'))

      const response = await PATCH(createRequest({ isArchived: true }), { params: Promise.resolve({ id: categoryId }) })
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Unable to archive category')
    })
  })
})
