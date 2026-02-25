import { describe, it, expect, beforeEach, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { resetAllRateLimits } from '@/lib/rate-limit'

// Mock dependencies
vi.mock('@/lib/auth-server', () => ({
  verifyCredentials: vi.fn().mockResolvedValue({ valid: false, reason: 'invalid_credentials' }),
}))

vi.mock('@/lib/prisma', () => ({
  prisma: {
    refreshToken: { create: vi.fn() },
    user: { findUnique: vi.fn() }
  },
}))

vi.mock('@/lib/services/registration-service', () => ({
  registerUser: vi.fn().mockResolvedValue({ success: true, emailVerified: false }),
}))

vi.mock('@/lib/email', () => ({
  sendVerificationEmail: vi.fn().mockResolvedValue({ success: true }),
}))

// Import handlers after mocking
import { POST as loginPost } from '@/app/api/v1/auth/login/route'
import { POST as registerPost } from '@/app/api/v1/auth/register/route'

describe('Auth IP Rate Limiting', () => {
  beforeEach(() => {
    resetAllRateLimits()
    vi.clearAllMocks()
  })

  describe('Login IP Rate Limit', () => {
    const buildLoginRequest = (ip: string, email: string) =>
      new NextRequest('http://localhost/api/v1/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-forwarded-for': ip,
        },
        body: JSON.stringify({ email, password: 'password123' }),
      })

    it('limits login attempts from same IP (credential stuffing protection)', async () => {
      const ip = '192.168.1.1'
      // Login limit is 20 per minute
      const limit = 20

      // Make 20 allowed requests with different emails
      for (let i = 0; i < limit; i++) {
        const response = await loginPost(buildLoginRequest(ip, `user${i}@example.com`))
        expect(response.status).not.toBe(429)
      }

      // 21st request should be blocked
      const response = await loginPost(buildLoginRequest(ip, 'another@example.com'))
      expect(response.status).toBe(429)
      const data = await response.json()
      expect(data).toMatchObject({ error: 'Rate limit exceeded' })
    })

    it('does not limit requests from different IPs', async () => {
      // Make 10 requests from IP 1
      for (let i = 0; i < 10; i++) {
        await loginPost(buildLoginRequest('1.1.1.1', `user${i}@example.com`))
      }

      // Make 10 requests from IP 2
      for (let i = 0; i < 10; i++) {
        const response = await loginPost(buildLoginRequest('2.2.2.2', `user${i}@example.com`))
        expect(response.status).not.toBe(429)
      }
    })
  })

  describe('Registration IP Rate Limit', () => {
    const buildRegisterRequest = (ip: string, email: string) =>
      new NextRequest('http://localhost/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-forwarded-for': ip,
        },
        body: JSON.stringify({
          email,
          password: 'Password123!',
          displayName: 'Test User'
        }),
      })

    it('limits registration attempts from same IP', async () => {
      const ip = '10.0.0.1'
      // Registration limit is 10 per hour
      const limit = 10

      for (let i = 0; i < limit; i++) {
        const response = await registerPost(buildRegisterRequest(ip, `newuser${i}@example.com`))
        expect(response.status).not.toBe(429)
      }

      // 11th request should be blocked
      const response = await registerPost(buildRegisterRequest(ip, 'fail@example.com'))
      expect(response.status).toBe(429)
    })
  })
})
