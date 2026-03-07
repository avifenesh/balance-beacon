import { NextRequest } from 'next/server'
import { requireJwtAuth } from '@/lib/api-auth'
import { deleteRecurringTemplate, getRecurringTemplateById } from '@/lib/services/recurring-service'
import {
  authError,
  checkSubscription,
  notFoundError,
  rateLimitError,
  serverError,
  successResponse,
} from '@/lib/api-helpers'
import { consumeRateLimit } from '@/lib/rate-limit'

/**
 * DELETE /api/v1/recurring/[id]
 *
 * Soft deletes a recurring template owned by the authenticated user.
 */
export async function DELETE(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params

  let user
  try {
    user = requireJwtAuth(request)
  } catch (error) {
    return authError(error instanceof Error ? error.message : 'Unauthorized')
  }

  const rateLimit = await consumeRateLimit(user.userId)
  if (!rateLimit.allowed) {
    return rateLimitError(rateLimit.resetAt)
  }

  const subscriptionError = await checkSubscription(user.userId)
  if (subscriptionError) return subscriptionError

  const existing = await getRecurringTemplateById(id, user.userId)
  if (!existing) {
    return notFoundError('Recurring template not found')
  }

  try {
    await deleteRecurringTemplate({
      id,
      deletedBy: user.userId,
    })
    return successResponse({ id, deleted: true })
  } catch {
    return serverError('Unable to delete recurring template')
  }
}
