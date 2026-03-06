import { describe, expect, it } from 'vitest'
import { SUBSCRIPTION_PRICE_CENTS, TRIAL_DURATION_DAYS } from '@/lib/subscription-constants'

describe('Subscription Constants', () => {
  describe('TRIAL_DURATION_DAYS', () => {
    it('should be 14 days', () => {
      expect(TRIAL_DURATION_DAYS).toBe(14)
    })

    it('should be a positive integer', () => {
      expect(Number.isInteger(TRIAL_DURATION_DAYS)).toBe(true)
      expect(TRIAL_DURATION_DAYS).toBeGreaterThan(0)
    })
  })

  describe('SUBSCRIPTION_PRICE_CENTS', () => {
    it('should be 300 cents', () => {
      expect(SUBSCRIPTION_PRICE_CENTS).toBe(300)
    })

    it('should equal $3.00 when converted to dollars', () => {
      const priceInDollars = SUBSCRIPTION_PRICE_CENTS / 100
      expect(priceInDollars).toBe(3)
    })

    it('should be a positive integer', () => {
      expect(Number.isInteger(SUBSCRIPTION_PRICE_CENTS)).toBe(true)
      expect(SUBSCRIPTION_PRICE_CENTS).toBeGreaterThan(0)
    })

    it('should be evenly divisible by 100 (whole cents to dollars conversion)', () => {
      expect(SUBSCRIPTION_PRICE_CENTS % 100).toBe(0)
    })
  })
})
