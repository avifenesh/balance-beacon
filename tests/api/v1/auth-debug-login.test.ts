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
  beforeEach(() => {
    vi.stubEnv('NODE_ENV', 'test')
    vi.clearAllMocks()
    prismaMock.user.upsert.mockResolvedValue({
      id: 'demo-user-id',
      displayName: 'Android Demo',
      preferredCurrency: 'USD',
      hasCompletedOnboarding: true,
    })
    prismaMock.account.upsert.mockResolvedValue({ id: 'demo-account-id' })
    prismaMock.user.update.mockResolvedValue({ id: 'demo-user-id' })
    prismaMock.category.upsert.mockImplementation(async ({ create }: { create: { name: string } }) => ({
      id: `category-${create.name.toLowerCase().replace(/\s+/g, '-')}`,
      name: create.name,
    }))
    prismaMock.transaction.deleteMany.mockResolvedValue({ count: 18 })
    prismaMock.transaction.createMany.mockResolvedValue({ count: 48 })
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
    prismaMock.subscription.upsert.mockResolvedValue({ id: 'demo-subscription-id' })
    prismaMock.refreshToken.deleteMany.mockResolvedValue({ count: 2 })
    prismaMock.refreshToken.create.mockResolvedValue({ id: 'demo-refresh-token-id' })
  })

  afterEach(() => {
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
    expect(prismaMock.account.upsert).toHaveBeenCalledOnce()
    expect(prismaMock.transaction.createMany).toHaveBeenCalledOnce()
    expect(prismaMock.holding.createMany).toHaveBeenCalledOnce()
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
