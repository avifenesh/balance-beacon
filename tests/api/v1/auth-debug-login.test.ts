import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { NextRequest } from 'next/server'

const { mockGenerateAccessToken, mockGenerateRefreshToken, prismaMock } = vi.hoisted(() => ({
  mockGenerateAccessToken: vi.fn(() => 'debug-access-token'),
  mockGenerateRefreshToken: vi.fn(() => ({
    token: 'debug-refresh-token',
    jti: 'debug-jti',
    expiresAt: new Date('2026-03-19T00:00:00.000Z'),
  })),
  prismaMock: {
    user: {
      upsert: vi.fn(),
    },
    account: {
      upsert: vi.fn(),
    },
    subscription: {
      upsert: vi.fn(),
    },
    refreshToken: {
      create: vi.fn(),
    },
  },
}))

vi.mock('@/lib/jwt', () => ({
  generateAccessToken: mockGenerateAccessToken,
  generateRefreshToken: mockGenerateRefreshToken,
}))

vi.mock('@/lib/prisma', () => ({
  prisma: prismaMock,
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
    prismaMock.subscription.upsert.mockResolvedValue({ id: 'demo-subscription-id' })
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
    expect(prismaMock.subscription.upsert).toHaveBeenCalledOnce()
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
