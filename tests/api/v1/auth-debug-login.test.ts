import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { NextRequest } from 'next/server'

const { mockGenerateAccessToken, mockGenerateRefreshToken, mockInvalidateDashboardCache, prismaMock } = vi.hoisted(
  () => ({
    mockGenerateAccessToken: vi.fn(() => 'debug-access-token'),
    mockGenerateRefreshToken: vi.fn(() => ({
      token: 'debug-refresh-token',
      jti: 'debug-jti',
      expiresAt: new Date('2026-03-19T00:00:00.000Z'),
    })),
    mockInvalidateDashboardCache: vi.fn(),
    prismaMock: {
      user: {
        upsert: vi.fn(),
        update: vi.fn(),
      },
      account: {
        upsert: vi.fn(),
      },
      category: {
        upsert: vi.fn(),
      },
      transaction: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
        create: vi.fn(),
      },
      budget: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
      },
      monthlyIncomeGoal: {
        deleteMany: vi.fn(),
        create: vi.fn(),
      },
      holding: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
      },
      recurringTemplate: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
      },
      stockPrice: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
      },
      sharedExpense: {
        create: vi.fn(),
      },
      transactionRequest: {
        deleteMany: vi.fn(),
        createMany: vi.fn(),
      },
      subscription: {
        upsert: vi.fn(),
      },
      refreshToken: {
        deleteMany: vi.fn(),
        create: vi.fn(),
      },
    },
  }),
)

vi.mock('@/lib/jwt', () => ({
  generateAccessToken: mockGenerateAccessToken,
  generateRefreshToken: mockGenerateRefreshToken,
}))

vi.mock('@/lib/prisma', () => ({
  prisma: prismaMock,
}))

vi.mock('@/lib/dashboard-cache', () => ({
  invalidateDashboardCache: mockInvalidateDashboardCache,
}))

import { POST as debugLoginPost } from '@/app/api/v1/auth/debug-login/route'

describe('POST /api/v1/auth/debug-login', () => {
  beforeEach(async () => {
    vi.stubEnv('NODE_ENV', 'test')
    vi.clearAllMocks()
    prismaMock.user.upsert.mockImplementation(async ({ where }: { where: { email: string } }) => {
      const email = where.email
      if (email === 'android-demo@balancebeacon.local') {
        return {
          id: 'demo-user-id',
          displayName: 'Android Demo',
          preferredCurrency: 'USD',
          hasCompletedOnboarding: true,
        }
      }
      if (email === 'android-maya@balancebeacon.local') {
        return {
          id: 'maya-user-id',
          displayName: 'Maya Chen',
          preferredCurrency: 'USD',
          hasCompletedOnboarding: true,
        }
      }
      return {
        id: 'liam-user-id',
        displayName: 'Liam Ortiz',
        preferredCurrency: 'USD',
        hasCompletedOnboarding: true,
      }
    })
    prismaMock.account.upsert.mockImplementation(async ({ where }: { where: { userId_name: { name: string } } }) => {
      switch (where.userId_name.name) {
        case 'Android Demo':
          return { id: 'demo-account-id' }
        case 'Maya Chen':
          return { id: 'maya-account-id' }
        default:
          return { id: 'liam-account-id' }
      }
    })
    prismaMock.user.update.mockResolvedValue({ id: 'demo-user-id' })
    prismaMock.category.upsert.mockImplementation(async ({ create }: { create: { name: string } }) => ({
      id: `category-${create.name.toLowerCase().replace(/\s+/g, '-')}`,
      name: create.name,
    }))
    prismaMock.transaction.deleteMany.mockResolvedValue({ count: 18 })
    prismaMock.transaction.createMany.mockResolvedValue({ count: 48 })
    prismaMock.transaction.create
      .mockResolvedValueOnce({ id: 'shared-transaction-1' })
      .mockResolvedValueOnce({ id: 'shared-transaction-2' })
      .mockResolvedValueOnce({ id: 'shared-transaction-3' })
    prismaMock.budget.deleteMany.mockResolvedValue({ count: 5 })
    prismaMock.budget.createMany.mockResolvedValue({ count: 5 })
    prismaMock.monthlyIncomeGoal.deleteMany.mockResolvedValue({ count: 1 })
    prismaMock.monthlyIncomeGoal.create.mockResolvedValue({ id: 'goal-id' })
    prismaMock.holding.deleteMany.mockResolvedValue({ count: 3 })
    prismaMock.holding.createMany.mockResolvedValue({ count: 3 })
    prismaMock.recurringTemplate.deleteMany.mockResolvedValue({ count: 3 })
    prismaMock.recurringTemplate.createMany.mockResolvedValue({ count: 3 })
    prismaMock.stockPrice.deleteMany.mockResolvedValue({ count: 3 })
    prismaMock.stockPrice.createMany.mockResolvedValue({ count: 3 })
    prismaMock.sharedExpense.create.mockResolvedValue({ id: 'shared-expense-id' })
    prismaMock.transactionRequest.deleteMany.mockResolvedValue({ count: 4 })
    prismaMock.transactionRequest.createMany.mockResolvedValue({ count: 2 })
    prismaMock.subscription.upsert.mockResolvedValue({ id: 'demo-subscription-id' })
    prismaMock.refreshToken.deleteMany.mockResolvedValue({ count: 2 })
    prismaMock.refreshToken.create.mockResolvedValue({ id: 'demo-refresh-token-id' })
  })

  afterEach(async () => {
    vi.unstubAllEnvs()
  })

  it('returns working tokens for a provisioned debug demo user outside production', async () => {
    const response = await debugLoginPost(
      new NextRequest('http://localhost/api/v1/auth/debug-login', {
        method: 'POST',
      }),
    )

    expect(response.status).toBe(200)
    expect(await response.json()).toEqual({
      success: true,
      data: {
        accessToken: 'debug-access-token',
        refreshToken: 'debug-refresh-token',
        expiresIn: 900,
        user: {
          id: 'demo-user-id',
          email: 'android-demo@balancebeacon.local',
          displayName: 'Android Demo',
          preferredCurrency: 'USD',
          hasCompletedOnboarding: true,
        },
      },
    })

    expect(prismaMock.user.upsert).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { email: 'android-demo@balancebeacon.local' },
      }),
    )
    expect(prismaMock.user.upsert).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { email: 'android-maya@balancebeacon.local' },
      }),
    )
    expect(prismaMock.account.upsert).toHaveBeenCalledTimes(3)
    expect(prismaMock.transaction.createMany).toHaveBeenCalledOnce()
    expect(prismaMock.transaction.create).toHaveBeenCalledTimes(3)
    expect(prismaMock.holding.createMany).toHaveBeenCalledOnce()
    expect(prismaMock.sharedExpense.create).toHaveBeenCalledTimes(3)
    expect(prismaMock.transactionRequest.createMany).toHaveBeenCalledOnce()
    expect(prismaMock.subscription.upsert).toHaveBeenCalledOnce()
    expect(mockInvalidateDashboardCache).toHaveBeenCalledWith({ accountId: 'demo-account-id' })
    expect(prismaMock.refreshToken.create).toHaveBeenCalledWith({
      data: {
        jti: 'debug-jti',
        userId: 'demo-user-id',
        email: 'android-demo@balancebeacon.local',
        expiresAt: new Date('2026-03-19T00:00:00.000Z'),
      },
    })
  })

  it('is disabled in production', async () => {
    vi.stubEnv('NODE_ENV', 'production')

    const response = await debugLoginPost(
      new NextRequest('http://localhost/api/v1/auth/debug-login', {
        method: 'POST',
      }),
    )

    expect(response.status).toBe(404)
    expect(await response.json()).toEqual({ error: 'Not found' })
    expect(prismaMock.user.upsert).not.toHaveBeenCalled()
    expect(mockGenerateAccessToken).not.toHaveBeenCalled()
  })
})
