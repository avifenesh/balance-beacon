import { NextRequest } from 'next/server'
import { withApiAuth, parseJsonBody } from '@/lib/api-middleware'
import {
  getActualIncomeForMonth,
  getMonthlyIncomeGoalByKey,
  upsertMonthlyIncomeGoal,
  deleteMonthlyIncomeGoal,
} from '@/lib/services/budget-service'
import { monthlyIncomeGoalApiSchema, deleteMonthlyIncomeGoalApiSchema } from '@/schemas/api'
import { ensureApiAccountOwnership } from '@/lib/api-auth-helpers'
import { prisma } from '@/lib/prisma'
import { formatDateForApi, getMonthStartFromKey } from '@/utils/date'
import { validationError, forbiddenError, notFoundError, serverError, successResponse } from '@/lib/api-helpers'
import { serverLogger } from '@/lib/server-logger'

/**
 * GET /api/v1/budgets/income-goal
 *
 * Retrieves monthly income goal + actual income progress for an account/month.
 */
export async function GET(request: NextRequest) {
  return withApiAuth(request, async (user) => {
    const { searchParams } = new URL(request.url)
    const accountId = searchParams.get('accountId')
    const monthKey = searchParams.get('monthKey')

    const parsed = deleteMonthlyIncomeGoalApiSchema.safeParse({ accountId, monthKey })
    if (!parsed.success) {
      return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
    }

    const ownership = await ensureApiAccountOwnership(parsed.data.accountId, user.userId)
    if (!ownership.allowed) {
      return forbiddenError('Access denied')
    }

    const month = getMonthStartFromKey(parsed.data.monthKey)

    try {
      const [monthGoal, actualIncome, accountDefaults] = await Promise.all([
        getMonthlyIncomeGoalByKey({
          accountId: parsed.data.accountId,
          month,
        }),
        getActualIncomeForMonth(parsed.data.accountId, month),
        prisma.account.findUnique({
          where: { id: parsed.data.accountId },
          select: {
            defaultIncomeGoal: true,
            defaultIncomeGoalCurrency: true,
            preferredCurrency: true,
          },
        }),
      ])

      const incomeGoal = monthGoal
        ? {
            accountId: parsed.data.accountId,
            month: formatDateForApi(monthGoal.month),
            amount: monthGoal.amount.toString(),
            currency: monthGoal.currency,
            notes: monthGoal.notes,
            isDefault: false,
          }
        : accountDefaults?.defaultIncomeGoal
          ? {
              accountId: parsed.data.accountId,
              month: formatDateForApi(month),
              amount: accountDefaults.defaultIncomeGoal.toString(),
              currency: accountDefaults.defaultIncomeGoalCurrency ?? accountDefaults.preferredCurrency ?? 'USD',
              notes: null,
              isDefault: true,
            }
          : null

      return successResponse({
        incomeGoal,
        actualIncome: actualIncome.toString(),
      })
    } catch (error) {
      serverLogger.error(
        'Failed to fetch monthly income goal',
        { action: 'GET /api/v1/budgets/income-goal', userId: user.userId },
        error,
      )
      return serverError('Unable to fetch monthly income goal')
    }
  })
}

/**
 * POST /api/v1/budgets/income-goal
 *
 * Creates or updates a monthly income goal for an account.
 */
export async function POST(request: NextRequest) {
  return withApiAuth(
    request,
    async (user) => {
      const body = await parseJsonBody(request)
      if (body === null) {
        return validationError({ body: ['Invalid JSON'] })
      }

      const parsed = monthlyIncomeGoalApiSchema.safeParse(body)
      if (!parsed.success) {
        return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
      }

      const data = parsed.data

      const ownership = await ensureApiAccountOwnership(data.accountId, user.userId)
      if (!ownership.allowed) {
        return forbiddenError('Access denied')
      }

      const month = getMonthStartFromKey(data.monthKey)

      try {
        const goal = await upsertMonthlyIncomeGoal({
          accountId: data.accountId,
          month,
          amount: data.amount,
          currency: data.currency,
          notes: data.notes,
          setAsDefault: data.setAsDefault,
        })
        return successResponse(
          {
            id: goal.id,
            accountId: goal.accountId,
            month: formatDateForApi(goal.month),
            amount: goal.amount.toString(),
            currency: goal.currency,
            notes: goal.notes,
            isDefault: false,
            setAsDefault: Boolean(data.setAsDefault),
          },
          201,
        )
      } catch (error) {
        serverLogger.error(
          'Failed to save monthly income goal',
          { action: 'POST /api/v1/budgets/income-goal', userId: user.userId },
          error,
        )
        return serverError('Unable to save monthly income goal')
      }
    },
    { requireSubscription: true },
  )
}

/**
 * DELETE /api/v1/budgets/income-goal
 *
 * Soft deletes a month-specific income goal.
 */
export async function DELETE(request: NextRequest) {
  return withApiAuth(
    request,
    async (user) => {
      const { searchParams } = new URL(request.url)
      const accountId = searchParams.get('accountId')
      const monthKey = searchParams.get('monthKey')

      const parsed = deleteMonthlyIncomeGoalApiSchema.safeParse({ accountId, monthKey })
      if (!parsed.success) {
        return validationError(parsed.error.flatten().fieldErrors as Record<string, string[]>)
      }

      const data = parsed.data
      const ownership = await ensureApiAccountOwnership(data.accountId, user.userId)
      if (!ownership.allowed) {
        return forbiddenError('Access denied')
      }

      const month = getMonthStartFromKey(data.monthKey)

      const existing = await getMonthlyIncomeGoalByKey({
        accountId: data.accountId,
        month,
      })
      if (!existing) {
        return notFoundError('Income goal not found')
      }

      try {
        await deleteMonthlyIncomeGoal({
          accountId: data.accountId,
          month,
          userId: user.userId,
        })
        return successResponse({ deleted: true })
      } catch (error) {
        serverLogger.error(
          'Failed to delete monthly income goal',
          { action: 'DELETE /api/v1/budgets/income-goal', userId: user.userId },
          error,
        )
        return serverError('Unable to delete monthly income goal')
      }
    },
    { requireSubscription: true },
  )
}
