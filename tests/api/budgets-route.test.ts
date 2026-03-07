import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits, consumeRateLimit } from '@/lib/rate-limit'

// Mock external dependencies before importing route handlers
vi.mock('@/lib/api-auth', () => ({
  requireJwtAuth: vi.fn(),
}))

vi.mock('@/lib/prisma', () => ({
  prisma: {
    budget: {
      findMany: vi.fn(),
      findFirst: vi.fn(),
    },
    transaction: {
      groupBy: vi.fn(),
    },
    account: {
      findFirst: vi.fn(),
    },
  },
}))

vi.mock('@/lib/server-logger', () => ({
  serverLogger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
  },
}))

vi.mock('@/lib/subscription', () => ({
  getSubscriptionState: vi.fn().mockResolvedValue({ canAccessApp: true }),
}))

vi.mock('@/lib/services/budget-service', () => ({
  upsertBudget: vi.fn(),
  deleteBudget: vi.fn(),
  getBudgetByKey: vi.fn(),
}))

// Import after mocks
import { GET, POST, DELETE } from '@/app/api/v1/budgets/route'
import { requireJwtAuth } from '@/lib/api-auth'
import { prisma } from '@/lib/prisma'
import { upsertBudget, deleteBudget, getBudgetByKey } from '@/lib/services/budget-service'

const mockRequireJwtAuth = vi.mocked(requireJwtAuth)
const mockBudgetFindMany = vi.mocked(prisma.budget.findMany)
const mockTransactionGroupBy = vi.mocked(prisma.transaction.groupBy)
const mockAccountFindFirst = vi.mocked(prisma.account.findFirst)
const mockUpsertBudget = vi.mocked(upsertBudget)
const mockDeleteBudget = vi.mocked(deleteBudget)
const mockGetBudgetByKey = vi.mocked(getBudgetByKey)

const mockUser = { userId: 'user-123', email: 'test@example.com' }

// A month Date stored as 2026-01-01T00:00:00.000Z
const MONTH_DATE = new Date('2026-01-01T00:00:00.000Z')

const mockCategory = {
  id: 'cat-1',
  name: 'Groceries',
  type: 'EXPENSE',
  color: '#FF5733',
}

function makeBudget(overrides: Record<string, unknown> = {}) {
  return {
    id: 'budget-1',
    accountId: 'acc-1',
    categoryId: 'cat-1',
    month: MONTH_DATE,
    planned: { toString: () => '500.00' },
    currency: 'USD',
    notes: null,
    category: mockCategory,
    ...overrides,
  }
}

function makeAccount(overrides: Record<string, unknown> = {}) {
  return {
    id: 'acc-1',
    userId: 'user-123',
    deletedAt: null,
    ...overrides,
  }
}

// ============================================================
// GET /api/v1/budgets
// ============================================================

describe('GET /api/v1/budgets', () => {
  function createRequest(params: Record<string, string> = {}) {
    const url = new URL('http://localhost:3000/api/v1/budgets')
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v))
    return new NextRequest(url.toString(), {
      method: 'GET',
      headers: { Authorization: 'Bearer valid-token' },
    })
  }

  beforeEach(async () => {
    vi.clearAllMocks()
    await resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockAccountFindFirst.mockResolvedValue(makeAccount() as any)
    mockBudgetFindMany.mockResolvedValue([])
    mockTransactionGroupBy.mockResolvedValue([])
  })

  afterEach(async () => {
    await resetAllRateLimits()
  })

  // ----------------------------------------------------------
  // Authentication
  // ----------------------------------------------------------

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid token')
    })

    it('returns 401 when auth throws generic Unauthorized error', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Unauthorized')
      })

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Unauthorized')
    })
  })

  // ----------------------------------------------------------
  // Rate Limiting
  // ----------------------------------------------------------

  describe('Rate Limiting', () => {
    it('blocks requests when rate limit is exceeded', async () => {
      for (let i = 0; i < 100; i++) {
        await consumeRateLimit(mockUser.userId, 'default')
      }

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(429)
      expect(data.error).toBe('Rate limit exceeded')
    })

    it('allows requests under the rate limit', async () => {
      const response = await GET(createRequest({ accountId: 'acc-1' }))
      expect(response.status).toBe(200)
    })
  })

  // ----------------------------------------------------------
  // Input Validation
  // ----------------------------------------------------------

  describe('Input Validation', () => {
    it('returns 400 when accountId is missing', async () => {
      const response = await GET(createRequest())
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.error).toBe('Validation failed')
      expect(data.fields.accountId).toBeDefined()
    })

    it('returns 400 when accountId is an empty string', async () => {
      const response = await GET(createRequest({ accountId: '' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.accountId).toBeDefined()
    })

    it('treats empty month param as absent and returns 200', async () => {
      const response = await GET(createRequest({ accountId: 'acc-1', month: '' }))
      expect(response.status).toBe(200)
    })

    it('does not reject malformed month without dash separator (no server-side format validation)', async () => {
      // getMonthStartFromKey returns Invalid Date for bad input but does not throw.
      // The route passes it through without validating the Date object.
      const response = await GET(createRequest({ accountId: 'acc-1', month: '202601' }))
      expect(response.status).toBe(200)
    })

    it('accepts a valid YYYY-MM month query param', async () => {
      const response = await GET(createRequest({ accountId: 'acc-1', month: '2026-01' }))
      expect(response.status).toBe(200)
    })

    it('accepts request without a month param (returns all months)', async () => {
      const response = await GET(createRequest({ accountId: 'acc-1' }))
      expect(response.status).toBe(200)
    })
  })

  // ----------------------------------------------------------
  // Authorization
  // ----------------------------------------------------------

  describe('Authorization', () => {
    it('returns 403 when user does not own the account', async () => {
      mockAccountFindFirst.mockResolvedValue(null)

      const response = await GET(createRequest({ accountId: 'acc-other' }))
      const data = await response.json()

      expect(response.status).toBe(403)
      expect(data.error).toBe('Access denied')
    })
  })

  // ----------------------------------------------------------
  // percentUsed Calculation
  // ----------------------------------------------------------

  describe('percentUsed calculation', () => {
    it('calculates percentUsed = Math.round((spent / planned) * 100) when planned > 0', async () => {
      const budget = makeBudget({ planned: { toString: () => '500.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 250, toNumber: () => 250 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.data.budgets[0].percentUsed).toBe(50) // (250 / 500) * 100 = 50
    })

    it('calculates percentUsed = 120 when over-budget (spent=600, planned=500)', async () => {
      const budget = makeBudget({ planned: { toString: () => '500.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 600, toNumber: () => 600 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(120) // not capped at 100
    })

    it('calculates percentUsed = 100 when planned = 0 and spent > 0', async () => {
      const budget = makeBudget({ planned: { toString: () => '0.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 150, toNumber: () => 150 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(100)
    })

    it('calculates percentUsed = 0 when planned = 0 and spent = 0', async () => {
      const budget = makeBudget({ planned: { toString: () => '0.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([])

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(0)
    })

    it('rounds percentUsed to nearest integer', async () => {
      // spent=1, planned=3 → 33.333... → rounds to 33
      const budget = makeBudget({ planned: { toString: () => '3.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 1, toNumber: () => 1 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(33)
    })

    it('calculates percentUsed = 0 for a budget with no matching transactions', async () => {
      const budget = makeBudget({ planned: { toString: () => '200.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([])

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(0)
      expect(data.data.budgets[0].spent).toBe('0.00')
    })

    it('isolates spent amounts per category and month key', async () => {
      const budget1 = makeBudget({ id: 'budget-1', categoryId: 'cat-1', planned: { toString: () => '100.00' } })
      const budget2 = makeBudget({
        id: 'budget-2',
        categoryId: 'cat-2',
        planned: { toString: () => '200.00' },
        category: { id: 'cat-2', name: 'Transport', type: 'EXPENSE', color: '#0000FF' },
      })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget1, budget2] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 80, toNumber: () => 80 } },
        },
        {
          categoryId: 'cat-2',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 160, toNumber: () => 160 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].percentUsed).toBe(80) // 80/100
      expect(data.data.budgets[1].percentUsed).toBe(80) // 160/200
    })

    it('uses Math.abs of transaction amount (handles negative DB values)', async () => {
      // Transaction amounts may be stored as negative numbers; route normalizes with Math.abs
      const budget = makeBudget({ planned: { toString: () => '400.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          // Simulate negative stored amount (expense stored as negative)
          _sum: { amount: { valueOf: () => -200, toNumber: () => -200 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      // Math.abs(-200) = 200; 200/400 = 50%
      expect(data.data.budgets[0].percentUsed).toBe(50)
      expect(data.data.budgets[0].spent).toBe('200.00')
    })
  })

  // ----------------------------------------------------------
  // Success Response Shape
  // ----------------------------------------------------------

  describe('Success Response', () => {
    it('returns 200 with empty budgets array when account has no budgets', async () => {
      mockBudgetFindMany.mockResolvedValue([])

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
      expect(data.data.budgets).toEqual([])
    })

    it('returns all expected budget fields in the response', async () => {
      const budget = makeBudget()
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([])

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(response.status).toBe(200)
      const b = data.data.budgets[0]
      expect(b).toMatchObject({
        id: 'budget-1',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        planned: '500.00',
        currency: 'USD',
        notes: null,
        category: mockCategory,
      })
      expect(typeof b.month).toBe('string')
      expect(typeof b.spent).toBe('string')
      expect(typeof b.percentUsed).toBe('number')
    })

    it('returns spent as a 2-decimal string', async () => {
      const budget = makeBudget({ planned: { toString: () => '300.00' } })
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockBudgetFindMany.mockResolvedValue([budget] as any)
      mockTransactionGroupBy.mockResolvedValue([
        {
          categoryId: 'cat-1',
          month: MONTH_DATE,
          _sum: { amount: { valueOf: () => 75.5, toNumber: () => 75.5 } },
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ] as any)

      const response = await GET(createRequest({ accountId: 'acc-1' }))
      const data = await response.json()

      expect(data.data.budgets[0].spent).toBe('75.50')
    })

    it('skips transaction groupBy query when no budgets are found', async () => {
      mockBudgetFindMany.mockResolvedValue([])

      await GET(createRequest({ accountId: 'acc-1' }))

      expect(mockTransactionGroupBy).not.toHaveBeenCalled()
    })

    it('filters by month when month query param is provided', async () => {
      mockBudgetFindMany.mockResolvedValue([])

      await GET(createRequest({ accountId: 'acc-1', month: '2026-01' }))

      expect(mockBudgetFindMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({
            accountId: 'acc-1',
            month: new Date(Date.UTC(2026, 0, 1)),
            deletedAt: null,
          }),
        }),
      )
    })

    it('does not include month filter when month param is absent', async () => {
      mockBudgetFindMany.mockResolvedValue([])

      await GET(createRequest({ accountId: 'acc-1' }))

      const whereArg = mockBudgetFindMany.mock.calls[0]?.[0]?.where
      expect(whereArg).not.toHaveProperty('month')
    })
  })
})

// ============================================================
// POST /api/v1/budgets
// ============================================================

describe('POST /api/v1/budgets', () => {
  const validBody = {
    accountId: 'acc-1',
    categoryId: 'cat-1',
    monthKey: '2026-01',
    planned: 500,
    currency: 'USD',
  }

  const savedBudget = {
    id: 'budget-new',
    accountId: 'acc-1',
    categoryId: 'cat-1',
    month: MONTH_DATE,
    planned: { toString: () => '500.00' },
    currency: 'USD',
    notes: null,
  }

  function createRequest(body: unknown) {
    return new NextRequest('http://localhost:3000/api/v1/budgets', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer valid-token',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })
  }

  beforeEach(async () => {
    vi.clearAllMocks()
    await resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockAccountFindFirst.mockResolvedValue(makeAccount() as any)
    mockGetBudgetByKey.mockResolvedValue(null) // no existing budget by default
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockUpsertBudget.mockResolvedValue(savedBudget as any)
  })

  afterEach(async () => {
    await resetAllRateLimits()
  })

  // ----------------------------------------------------------
  // Authentication
  // ----------------------------------------------------------

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid token')
    })

    it('returns 401 when auth throws generic Unauthorized error', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Unauthorized')
      })

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Unauthorized')
    })
  })

  // ----------------------------------------------------------
  // Subscription Gate
  // ----------------------------------------------------------

  describe('Subscription', () => {
    it('returns 402 when user subscription is inactive', async () => {
      const { getSubscriptionState } = await import('@/lib/subscription')
      vi.mocked(getSubscriptionState).mockResolvedValueOnce({ canAccessApp: false } as ReturnType<
        typeof getSubscriptionState
      > extends Promise<infer T>
        ? T
        : never)

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(402)
      expect(data.code).toBe('SUBSCRIPTION_REQUIRED')
    })
  })

  // ----------------------------------------------------------
  // Input Validation
  // ----------------------------------------------------------

  describe('Input Validation', () => {
    it('returns 400 for invalid JSON body', async () => {
      const request = new NextRequest('http://localhost:3000/api/v1/budgets', {
        method: 'POST',
        headers: {
          Authorization: 'Bearer valid-token',
          'Content-Type': 'application/json',
        },
        body: 'not-json{{{',
      })

      const response = await POST(request)
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.body).toBeDefined()
    })

    it('returns 400 when accountId is missing', async () => {
      const { accountId: _, ...bodyWithoutAccount } = validBody
      void _
      const response = await POST(createRequest(bodyWithoutAccount))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.accountId).toBeDefined()
    })

    it('returns 400 when categoryId is missing', async () => {
      const { categoryId: _, ...bodyWithoutCategory } = validBody
      void _
      const response = await POST(createRequest(bodyWithoutCategory))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.categoryId).toBeDefined()
    })

    it('returns 400 when monthKey is missing', async () => {
      const { monthKey: _, ...bodyWithoutMonth } = validBody
      void _
      const response = await POST(createRequest(bodyWithoutMonth))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.monthKey).toBeDefined()
    })

    it('returns 400 when planned is negative', async () => {
      const response = await POST(createRequest({ ...validBody, planned: -1 }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.planned).toBeDefined()
    })

    it('returns 400 for an invalid currency', async () => {
      const response = await POST(createRequest({ ...validBody, currency: 'GBP' }))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.currency).toBeDefined()
    })

    it('accepts planned = 0 (zero-based budget)', async () => {
      const response = await POST(createRequest({ ...validBody, planned: 0 }))
      expect(response.status).toBe(201)
    })

    it('accepts notes field when provided', async () => {
      const response = await POST(createRequest({ ...validBody, notes: 'Monthly groceries cap' }))
      expect(response.status).toBe(201)
    })

    it('accepts EUR as a valid currency', async () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockUpsertBudget.mockResolvedValue({ ...savedBudget, currency: 'EUR' } as any)
      const response = await POST(createRequest({ ...validBody, currency: 'EUR' }))
      expect(response.status).toBe(201)
    })

    it('accepts ILS as a valid currency', async () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mockUpsertBudget.mockResolvedValue({ ...savedBudget, currency: 'ILS' } as any)
      const response = await POST(createRequest({ ...validBody, currency: 'ILS' }))
      expect(response.status).toBe(201)
    })
  })

  // ----------------------------------------------------------
  // Authorization
  // ----------------------------------------------------------

  describe('Authorization', () => {
    it('returns 403 when user does not own the account', async () => {
      mockAccountFindFirst.mockResolvedValue(null)

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(403)
      expect(data.error).toBe('Access denied')
    })
  })

  // ----------------------------------------------------------
  // 201 vs 200 Status Code Logic
  // ----------------------------------------------------------

  describe('Create vs Update status code', () => {
    it('returns 201 when no existing budget is found (create)', async () => {
      mockGetBudgetByKey.mockResolvedValue(null)

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(201)
      expect(data.success).toBe(true)
    })

    it('returns 200 when an existing budget is found (update)', async () => {
      mockGetBudgetByKey.mockResolvedValue({
        id: 'budget-existing',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        month: MONTH_DATE,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any)

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
    })
  })

  // ----------------------------------------------------------
  // Success Response Shape
  // ----------------------------------------------------------

  describe('Success Response', () => {
    it('returns all expected budget fields', async () => {
      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(201)
      const b = data.data
      expect(b).toMatchObject({
        id: 'budget-new',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        planned: '500.00',
        currency: 'USD',
        notes: null,
      })
      expect(typeof b.month).toBe('string')
    })

    it('calls upsertBudget with correct arguments', async () => {
      await POST(createRequest(validBody))

      expect(mockUpsertBudget).toHaveBeenCalledWith({
        accountId: 'acc-1',
        categoryId: 'cat-1',
        month: new Date(Date.UTC(2026, 0, 1)),
        planned: 500,
        currency: 'USD',
        notes: undefined,
      })
    })

    it('checks budget existence before upsert to determine status code', async () => {
      await POST(createRequest(validBody))

      expect(mockGetBudgetByKey).toHaveBeenCalledWith({
        accountId: 'acc-1',
        categoryId: 'cat-1',
        month: new Date(Date.UTC(2026, 0, 1)),
      })
    })
  })

  // ----------------------------------------------------------
  // Error Handling
  // ----------------------------------------------------------

  describe('Error Handling', () => {
    it('returns 500 when upsertBudget throws an unexpected error', async () => {
      mockUpsertBudget.mockRejectedValue(new Error('Database write failed'))

      const response = await POST(createRequest(validBody))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Unable to save budget')
    })
  })
})

// ============================================================
// DELETE /api/v1/budgets
// ============================================================

describe('DELETE /api/v1/budgets', () => {
  const validParams = {
    accountId: 'acc-1',
    categoryId: 'cat-1',
    monthKey: '2026-01',
  }

  const existingBudget = {
    id: 'budget-1',
    accountId: 'acc-1',
    categoryId: 'cat-1',
    month: MONTH_DATE,
  }

  function createRequest(params: Record<string, string> = {}) {
    const url = new URL('http://localhost:3000/api/v1/budgets')
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v))
    return new NextRequest(url.toString(), {
      method: 'DELETE',
      headers: { Authorization: 'Bearer valid-token' },
    })
  }

  beforeEach(async () => {
    vi.clearAllMocks()
    await resetAllRateLimits()
    mockRequireJwtAuth.mockReturnValue(mockUser)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockAccountFindFirst.mockResolvedValue(makeAccount() as any)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockGetBudgetByKey.mockResolvedValue(existingBudget as any)
    mockDeleteBudget.mockResolvedValue(undefined as never)
  })

  afterEach(async () => {
    await resetAllRateLimits()
  })

  // ----------------------------------------------------------
  // Authentication
  // ----------------------------------------------------------

  describe('Authentication', () => {
    it('returns 401 when JWT is invalid', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Invalid token')
      })

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Invalid token')
    })

    it('returns 401 when auth throws generic Unauthorized error', async () => {
      mockRequireJwtAuth.mockImplementation(() => {
        throw new Error('Unauthorized')
      })

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(401)
      expect(data.error).toBe('Unauthorized')
    })
  })

  // ----------------------------------------------------------
  // Subscription Gate
  // ----------------------------------------------------------

  describe('Subscription', () => {
    it('returns 402 when user subscription is inactive', async () => {
      const { getSubscriptionState } = await import('@/lib/subscription')
      vi.mocked(getSubscriptionState).mockResolvedValueOnce({ canAccessApp: false } as ReturnType<
        typeof getSubscriptionState
      > extends Promise<infer T>
        ? T
        : never)

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(402)
      expect(data.code).toBe('SUBSCRIPTION_REQUIRED')
    })
  })

  // ----------------------------------------------------------
  // Input Validation
  // ----------------------------------------------------------

  describe('Input Validation', () => {
    it('returns 400 when accountId is missing', async () => {
      const { accountId: _, ...rest } = validParams
      void _
      const response = await DELETE(createRequest(rest))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.accountId).toBeDefined()
    })

    it('returns 400 when categoryId is missing', async () => {
      const { categoryId: _, ...rest } = validParams
      void _
      const response = await DELETE(createRequest(rest))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.categoryId).toBeDefined()
    })

    it('returns 400 when monthKey is missing', async () => {
      const { monthKey: _, ...rest } = validParams
      void _
      const response = await DELETE(createRequest(rest))
      const data = await response.json()

      expect(response.status).toBe(400)
      expect(data.fields.monthKey).toBeDefined()
    })
  })

  // ----------------------------------------------------------
  // Authorization
  // ----------------------------------------------------------

  describe('Authorization', () => {
    it('returns 403 when user does not own the account', async () => {
      mockAccountFindFirst.mockResolvedValue(null)

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(403)
      expect(data.error).toBe('Access denied')
    })
  })

  // ----------------------------------------------------------
  // 404 when budget not found
  // ----------------------------------------------------------

  describe('Budget Not Found', () => {
    it('returns 404 when budget does not exist', async () => {
      mockGetBudgetByKey.mockResolvedValue(null)

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(404)
      expect(data.error).toBe('Budget entry not found')
    })
  })

  // ----------------------------------------------------------
  // Success
  // ----------------------------------------------------------

  describe('Success', () => {
    it('returns 200 with { deleted: true } on successful delete', async () => {
      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
      expect(data.data.deleted).toBe(true)
    })

    it('calls deleteBudget with correct arguments including userId', async () => {
      await DELETE(createRequest(validParams))

      expect(mockDeleteBudget).toHaveBeenCalledWith({
        accountId: 'acc-1',
        categoryId: 'cat-1',
        month: new Date(Date.UTC(2026, 0, 1)),
        userId: 'user-123',
      })
    })

    it('checks budget existence before attempting delete', async () => {
      await DELETE(createRequest(validParams))

      expect(mockGetBudgetByKey).toHaveBeenCalledWith({
        accountId: 'acc-1',
        categoryId: 'cat-1',
        month: new Date(Date.UTC(2026, 0, 1)),
      })
      expect(mockDeleteBudget).toHaveBeenCalled()
    })

    it('does not call deleteBudget when budget is not found', async () => {
      mockGetBudgetByKey.mockResolvedValue(null)

      await DELETE(createRequest(validParams))

      expect(mockDeleteBudget).not.toHaveBeenCalled()
    })
  })

  // ----------------------------------------------------------
  // Error Handling
  // ----------------------------------------------------------

  describe('Error Handling', () => {
    it('returns 500 when deleteBudget throws an unexpected error', async () => {
      mockDeleteBudget.mockRejectedValue(new Error('Constraint violation'))

      const response = await DELETE(createRequest(validParams))
      const data = await response.json()

      expect(response.status).toBe(500)
      expect(data.error).toBe('Unable to delete budget')
    })
  })
})
