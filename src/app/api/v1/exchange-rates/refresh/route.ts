import { NextRequest } from 'next/server'
import { requireJwtAuth } from '@/lib/api-auth'
import { authError, rateLimitError, serverError, successResponse } from '@/lib/api-helpers'
import { consumeRateLimit } from '@/lib/rate-limit'
import { refreshExchangeRates } from '@/lib/currency'
import { serverLogger } from '@/lib/server-logger'

/**
 * POST /api/v1/exchange-rates/refresh
 *
 * Refreshes exchange rates for all supported currency pairs.
 *
 * @returns {Object} { updatedAt: string } ISO timestamp of refresh attempt
 * @throws {401} Unauthorized - Invalid or missing auth token
 * @throws {429} Rate limited - Too many requests
 * @throws {500} Server error - Unable to refresh rates
 */
export async function POST(request: NextRequest) {
  // 1. Authenticate
  let user
  try {
    user = requireJwtAuth(request)
  } catch (error) {
    return authError(error instanceof Error ? error.message : 'Unauthorized')
  }

  // 1.5 Rate limit check
  const rateLimit = await consumeRateLimit(user.userId)
  if (!rateLimit.allowed) {
    return rateLimitError(rateLimit.resetAt)
  }

  // Note: No subscription check - exchange-rate refresh is allowed for authenticated users

  // 2. Execute refresh
  try {
    const result = await refreshExchangeRates()
    if ('error' in result) {
      return serverError(result.error.general[0] ?? 'Unable to refresh exchange rates')
    }

    return successResponse({
      updatedAt: result.updatedAt.toISOString(),
    })
  } catch (error) {
    serverLogger.error('Failed to refresh exchange rates', { action: 'POST /api/v1/exchange-rates/refresh' }, error)
    return serverError('Unable to refresh exchange rates')
  }
}
