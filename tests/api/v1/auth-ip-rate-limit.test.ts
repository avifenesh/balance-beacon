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
    user: { findUnique: vi.fn() },
  },
}))

vi.mock('@/lib/services/registration-service', () => ({
  registerUser: vi.fn().mockResolvedValue({
    success: true,
    emailVerified: false,
    verificationToken: 'token',
    verificationExpires: new Date()
  }),
}))

vi.mock('@/lib/email', () => ({
  sendVerificationEmail: vi.fn().mockResolvedValue({ success: true }),
}))

import { POST as loginPost } from '@/app/api/v1/auth/login/route'
import { POST as registerPost } from '@/app/api/v1/auth/register/route'

describe('Auth IP Rate Limiting', () => {
  beforeEach(() => {
    resetAllRateLimits()
    vi.clearAllMocks()
  })

  describe('Login IP Rate Limiting', () => {
    it('blocks requests from same IP after limit (20)', async () => {
      const ip = '192.168.1.1'
      const buildRequest = (email: string) =>
        new NextRequest('http://localhost/api/v1/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-forwarded-for': ip
          },
          body: JSON.stringify({ email, password: 'bad-password' }),
        })

      // Make 20 allowed requests (using different emails to avoid email-based limit of 5)
      for (let i = 0; i < 20; i++) {
        const response = await loginPost(buildRequest(`user${i}@example.com`))
        // Expect 401 (auth failed) or 200, but NOT 429
        expect(response.status).not.toBe(429)
      }

      // 21st request should be blocked
      const response = await loginPost(buildRequest('another@example.com'))
      expect(response.status).toBe(429)
      const data = await response.json()
      expect(data.error).toBe('Rate limit exceeded')
    })

    it('allows requests from different IPs', async () => {
      const buildRequest = (ip: string, email: string) =>
        new NextRequest('http://localhost/api/v1/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-forwarded-for': ip
          },
          body: JSON.stringify({ email, password: 'bad-password' }),
        })

      // Exhaust IP limit for IP 1
      for (let i = 0; i < 20; i++) {
        await loginPost(buildRequest('1.1.1.1', `userA${i}@example.com`))
      }

      const blockedResponse = await loginPost(buildRequest('1.1.1.1', 'blocked@example.com'))
      expect(blockedResponse.status).toBe(429)

      // Request from IP 2 should still work
      const allowedResponse = await loginPost(buildRequest('2.2.2.2', 'allowed@example.com'))
      expect(allowedResponse.status).not.toBe(429)
    })
  });

  describe('Registration IP Rate Limiting', () => {
    it('blocks requests from same IP after limit (10)', async () => {
      const ip = '10.0.0.1'
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

      // Make 10 allowed requests (different emails)
      for (let i = 0; i < 10; i++) {
        const response = await registerPost(buildRequest(`reg${i}@example.com`))
        expect(response.status).not.toBe(429)
      }

      // 11th request should be blocked
      const response = await registerPost(buildRequest('blocked@example.com'))
      expect(response.status).toBe(429)
    })

    it('prioritizes IP limit check over email limit check', async () => {
        // This test ensures IP limiting works even if email limit isn't hit
        const ip = '10.0.0.5'
        // Exhaust IP limit
        for (let i = 0; i < 10; i++) {
            await registerPost(new NextRequest('http://localhost/api/v1/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'x-forwarded-for': ip },
                body: JSON.stringify({ email: `u${i}@example.com`, password: 'Password123!', displayName: 'Test' })
            }))
        }

        // Next request with a fresh email (so email limit not hit) should still be blocked by IP
        const response = await registerPost(new NextRequest('http://localhost/api/v1/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'x-forwarded-for': ip },
            body: JSON.stringify({ email: `fresh@example.com`, password: 'Password123!', displayName: 'Test' })
        }))

        expect(response.status).toBe(429)
    })
  });
})
