import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

// Mock dependencies
vi.mock('@/lib/auth-server', () => ({
  verifyCredentials: vi.fn().mockResolvedValue({ valid: false, reason: 'invalid_credentials' }),
}))

vi.mock('@/lib/prisma', () => ({
  prisma: {
    refreshToken: {
      create: vi.fn(),
    },
    user: {
      findUnique: vi.fn(),
    },
  },
}))

// Import handlers after mocking
import { POST as loginPost } from '@/app/api/v1/auth/login/route'
import { POST as registerPost } from '@/app/api/v1/auth/register/route'

describe('Auth IP Rate Limiting', () => {
  beforeEach(() => {
    resetAllRateLimits()
    vi.clearAllMocks()
  })

  const buildLoginRequest = (ip: string, email: string) =>
    new NextRequest('http://localhost/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-forwarded-for': ip,
      },
      body: JSON.stringify({ email, password: 'password123' }),
    })

  const buildRegisterRequest = (ip: string, email: string) =>
    new NextRequest('http://localhost/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-forwarded-for': ip,
      },
      body: JSON.stringify({
        email,
        password: 'Password123',
        displayName: 'Test User',
      }),
    })

  it('limits login attempts from the same IP (max 20/min)', async () => {
    const ip = '192.168.1.1'

    // First 20 attempts should be allowed (by IP limit)
    // Note: They might fail the per-email limit (5/min), so we switch emails every request
    for (let i = 0; i < 20; i++) {
      const email = `user${i}@example.com`
      const response = await loginPost(buildLoginRequest(ip, email))
      // Should not be 429 from IP limit
      // It might be 401 (invalid creds) or 429 (per-email limit if we reused email)
      expect(response.status).not.toBe(429)
    }

    // 21st attempt from same IP should be blocked
    const response = await loginPost(buildLoginRequest(ip, 'another@example.com'))
    expect(response.status).toBe(429)
    const data = await response.json()
    expect(data.error).toBe('Rate limit exceeded')
  })

  it('limits registration attempts from the same IP (max 20/min)', async () => {
    const ip = '192.168.1.2'

    // Mock registration service to avoid actual DB calls/email sending
    vi.mock('@/lib/services/registration-service', () => ({
      registerUser: vi.fn().mockResolvedValue({ success: false, reason: 'mocked_fail' }),
    }))

    // First 20 attempts
    for (let i = 0; i < 20; i++) {
      const email = `newuser${i}@example.com`
      const response = await registerPost(buildRegisterRequest(ip, email))
      expect(response.status).not.toBe(429)
    }

    // 21st attempt
    const response = await registerPost(buildRegisterRequest(ip, 'another_new@example.com'))
    expect(response.status).toBe(429)
  })

  it('counts login and registration towards the same IP limit', async () => {
    const ip = '192.168.1.3'

    // 10 logins
    for (let i = 0; i < 10; i++) {
      await loginPost(buildLoginRequest(ip, `login${i}@example.com`))
    }

    // 10 registrations
    for (let i = 0; i < 10; i++) {
      await registerPost(buildRegisterRequest(ip, `register${i}@example.com`))
    }

    // 21st request (login) should be blocked
    const response = await loginPost(buildLoginRequest(ip, 'overflow@example.com'))
    expect(response.status).toBe(429)
  })

  it('allows requests from different IPs', async () => {
    // 20 requests from IP 1
    for (let i = 0; i < 20; i++) {
      await loginPost(buildLoginRequest('10.0.0.1', `userA${i}@example.com`))
    }

    // Request from IP 2 should still be allowed
    const response = await loginPost(buildLoginRequest('10.0.0.2', 'userB@example.com'))
    expect(response.status).not.toBe(429)
  })

  it('handles x-forwarded-for with multiple IPs (takes first)', async () => {
    const clientIp = '203.0.113.1'
    const proxyIp = '198.51.100.1'
    const headerVal = `${clientIp}, ${proxyIp}`

    // Exhaust limit for clientIp
    for (let i = 0; i < 20; i++) {
      await loginPost(buildLoginRequest(headerVal, `proxyUser${i}@example.com`))
    }

    // Should be blocked
    const response = await loginPost(buildLoginRequest(headerVal, 'proxyUserFinal@example.com'))
    expect(response.status).toBe(429)

    // Request from the proxy IP itself (if it were the client) should be allowed
    // (proving we used the first IP in the list)
    const proxyResponse = await loginPost(buildLoginRequest(proxyIp, 'admin@proxy.com'))
    expect(proxyResponse.status).not.toBe(429)
  })
})
