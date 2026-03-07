import { resetAllRateLimits } from '@/lib/rate-limit'
import { resetEnvCache } from '@/lib/env-schema'

/**
 * Standard setup for API integration tests.
 * Call this in beforeEach for consistent test state.
 */
export function setupApiTest() {
  process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
  resetEnvCache()
  resetAllRateLimits()
}
