'use server'

import { Prisma, TransactionType } from '@prisma/client'
import { revalidatePath } from 'next/cache'
import { z } from 'zod'
import { prisma } from '@/lib/prisma'
import { getMonthStartFromKey } from '@/utils/date'
import { refreshExchangeRates } from '@/lib/currency'
import { success, generalError } from '@/lib/action-result'
import { handlePrismaError } from '@/lib/prisma-errors'
import { serverLogger } from '@/lib/server-logger'
import { requireSession } from '@/lib/auth-server'
import { parseInput, toDecimalString, ensureAccountAccess, requireCsrfToken } from './shared'
import { refreshExchangeRatesSchema, setBalanceSchema } from '@/schemas'
import { invalidateDashboardCache } from '@/lib/dashboard-cache'

export async function refreshExchangeRatesAction(input: z.infer<typeof refreshExchangeRatesSchema>) {
  const parsed = parseInput(refreshExchangeRatesSchema, input)
  if ('error' in parsed) return parsed

  const csrfCheck = await requireCsrfToken(parsed.data.csrfToken)
  if ('error' in csrfCheck) return csrfCheck

  try {
    await requireSession()
  } catch {
    return generalError('Your session expired. Please sign in again.')
  }

  try {
    const result = await refreshExchangeRates()
    if ('error' in result) {
      return result
    }

    revalidatePath('/')
    return success({ updatedAt: result.updatedAt })
  } catch (error) {
    serverLogger.error('Failed to refresh exchange rates', { action: 'refreshExchangeRates' }, error)
    return generalError('Unable to refresh exchange rates')
  }
}

export async function setBalanceAction(input: z.infer<typeof setBalanceSchema>) {
  const parsed = parseInput(setBalanceSchema, input)
  if ('error' in parsed) return parsed
  const { accountId, targetBalance, currency, monthKey, csrfToken } = parsed.data

  const csrfCheck = await requireCsrfToken(csrfToken)
  if ('error' in csrfCheck) return csrfCheck

  const access = await ensureAccountAccess(accountId)
  if ('error' in access) {
    return access
  }
  const { authUser } = access

  const monthStart = getMonthStartFromKey(monthKey)

  try {
    // Use atomic transaction to prevent race conditions between aggregate and create
    const result = await prisma.$transaction(async (tx) => {
      // Use aggregate instead of findMany to push summation to the database
      const [incomeAgg, expenseAgg] = await Promise.all([
        tx.transaction.aggregate({
          where: { accountId, month: monthStart, deletedAt: null, type: TransactionType.INCOME },
          _sum: { amount: true },
        }),
        tx.transaction.aggregate({
          where: { accountId, month: monthStart, deletedAt: null, type: TransactionType.EXPENSE },
          _sum: { amount: true },
        }),
      ])

      const currentIncome = incomeAgg._sum.amount ? Number(incomeAgg._sum.amount) : 0
      const currentExpense = expenseAgg._sum.amount ? Number(expenseAgg._sum.amount) : 0
      const currentNet = currentIncome - currentExpense
      const adjustment = targetBalance - currentNet

      if (Math.abs(adjustment) < 0.01) {
        return { adjustment: 0 }
      }

      const transactionType = adjustment > 0 ? TransactionType.INCOME : TransactionType.EXPENSE
      const transactionAmount = Math.abs(adjustment)

      const adjustmentCategory = await tx.category.upsert({
        where: {
          userId_name_type: {
            userId: authUser.id,
            name: 'Balance Adjustment',
            type: transactionType,
          },
        },
        create: {
          userId: authUser.id,
          name: 'Balance Adjustment',
          type: transactionType,
        },
        update: {},
        select: { id: true },
      })

      await tx.transaction.create({
        data: {
          accountId,
          categoryId: adjustmentCategory.id,
          type: transactionType,
          amount: new Prisma.Decimal(toDecimalString(transactionAmount)),
          currency,
          date: new Date(),
          month: monthStart,
          description: 'Balance adjustment',
          isRecurring: false,
        },
      })

      return { adjustment }
    })

    if (Math.abs(result.adjustment) >= 0.01) {
      await invalidateDashboardCache({ monthKey, accountId })
      revalidatePath('/')
    }

    return success({ adjustment: result.adjustment })
  } catch (error) {
    return handlePrismaError(error, {
      action: 'setBalance',
      accountId,
      input: { targetBalance, currency, monthKey },
      fallbackMessage: 'Unable to create balance adjustment',
    })
  }
}
