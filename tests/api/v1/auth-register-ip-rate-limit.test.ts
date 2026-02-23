import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

// Mock dependencies
vi.mock('@/lib/services/registration-service', () => ({
  registerUser: vi.fn().mockResolvedValue({ success: true, emailVerified: false }),
}))
vi.mock('@/lib/email', () => ({
  sendVerificationEmail: vi.fn().mockResolvedValue({ success: true }),
}))
vi.mock('@/lib/server-logger', () => ({
  serverLogger: {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  },
}))

import { POST as registerPost } from '@/app/api/v1/auth/register/route'

describe('Auth registration rate limiting by IP', () => {
  beforeEach(() => {
    resetAllRateLimits()
    vi.clearAllMocks()
  })

  it('limits registration attempts by IP address (3 per minute)', async () => {
    const ip = '192.168.1.1'
    const buildRequest = (email: string) =>
      new NextRequest('http://localhost/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-forwarded-for': ip
        },
        body: JSON.stringify({
          email,
          password: 'Password123!',
          displayName: 'Test User'
        }),
      })

    // 1st attempt
    const res1 = await registerPost(buildRequest('user1@example.com'))
    expect(res1.status).toBe(201)

    // 2nd attempt
    const res2 = await registerPost(buildRequest('user2@example.com'))
    expect(res2.status).toBe(201)

    // 3rd attempt
    const res3 = await registerPost(buildRequest('user3@example.com'))
    expect(res3.status).toBe(201)

    // 4th attempt - should fail due to IP rate limit
    const res4 = await registerPost(buildRequest('user4@example.com'))
    expect(res4.status).toBe(429)

    const body = await res4.json()
    expect(body.error).toBe('Rate limit exceeded')
  })

  it('allows requests from different IPs', async () => {
    const buildRequest = (email: string, ip: string) =>
      new NextRequest('http://localhost/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-forwarded-for': ip
        },
        body: JSON.stringify({
          email,
          password: 'Password123!',
          displayName: 'Test User'
        }),
      })

    // IP 1: 3 attempts
    await registerPost(buildRequest('user1@example.com', '10.0.0.1'))
    await registerPost(buildRequest('user2@example.com', '10.0.0.1'))
    await registerPost(buildRequest('user3@example.com', '10.0.0.1'))

    // IP 1: 4th attempt fails
    const resIp1 = await registerPost(buildRequest('user4@example.com', '10.0.0.1'))
    expect(resIp1.status).toBe(429)

    // IP 2: 1st attempt succeeds (independent limit)
    const resIp2 = await registerPost(buildRequest('user5@example.com', '10.0.0.2'))
    expect(resIp2.status).toBe(201)
  })

  it('respects email rate limit as well', async () => {
    const ip = '192.168.1.5'
    const email = 'spam@example.com'
    const buildRequest = () =>
      new NextRequest('http://localhost/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-forwarded-for': ip
        },
        body: JSON.stringify({
          email,
          password: 'Password123!',
          displayName: 'Test User'
        }),
      })

    // 1st attempt
    await registerPost(buildRequest())
    // 2nd attempt
    await registerPost(buildRequest())
    // 3rd attempt
    await registerPost(buildRequest())

    // 4th attempt - fails (either by IP or email, both limits are 3)
    const res4 = await registerPost(buildRequest())
    expect(res4.status).toBe(429)
  })
})
