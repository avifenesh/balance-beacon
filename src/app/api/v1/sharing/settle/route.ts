import { NextRequest } from 'next/server'
import { PaymentStatus } from '@prisma/client'
import { withApiAuth, parseJsonBody } from '@/lib/api-middleware'
import { prisma } from '@/lib/prisma'
import { settleAllWithUserSchema } from '@/schemas'
import { successResponse, validationError } from '@/lib/api-helpers'

/**
 * POST /api/v1/sharing/settle
 *
 * Settles all pending shares for a target user and currency where the caller is the expense owner.
 * This mirrors the same business logic used by settleAllWithUserAction.
 */
export async function POST(request: NextRequest) {
  return withApiAuth(
    request,
    async (user) => {
      const body = await parseJsonBody(request)
      if (body === null) {
        return validationError({ body: ['Invalid JSON'] })
      }

      const parsed = settleAllWithUserSchema.omit({ csrfToken: true }).safeParse(body)
      if (!parsed.success) {
        return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
      }

      const data = parsed.data

      const participantsToSettle = await prisma.expenseParticipant.findMany({
        where: {
          status: PaymentStatus.PENDING,
          userId: data.targetUserId,
          sharedExpense: {
            ownerId: user.userId,
            currency: data.currency,
            deletedAt: null,
          },
          deletedAt: null,
        },
        select: { id: true },
      })

      const participantIds = participantsToSettle.map((p) => p.id)

      if (participantIds.length === 0) {
        return validationError({
          targetUserId: ['No pending payments to receive from this user'],
        })
      }

      const updateResult = await prisma.expenseParticipant.updateMany({
        where: {
          id: { in: participantIds },
          status: PaymentStatus.PENDING,
        },
        data: {
          status: PaymentStatus.PAID,
          paidAt: new Date(),
        },
      })

      return successResponse({
        settledCount: updateResult.count,
      })
    },
    { requireSubscription: true },
  )
}
