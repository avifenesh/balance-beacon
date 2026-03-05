import { NextRequest } from 'next/server'
import { withApiAuth } from '@/lib/api-middleware'
import {
  getSharedExpenses,
  getExpensesSharedWithMe,
  getSettlementBalance,
  getPaymentHistory,
} from '@/lib/finance/expense-sharing'
import { successResponse } from '@/lib/api-helpers'
import { formatSharedExpense, formatParticipation, formatSettlementBalance } from '@/app/api/v1/expenses/formatters'

/**
 * GET /api/v1/sharing
 *
 * Retrieves all sharing data for the authenticated user:
 * - Expenses they have shared with others
 * - Expenses shared with them by others
 * - Settlement balances with each person they share with
 *
 * @returns {Object} { sharedExpenses, expensesSharedWithMe, settlementBalances, paymentHistory }
 * @throws {401} Unauthorized - Invalid or missing auth token
 * @throws {429} Rate limited - Too many requests
 */
export async function GET(request: NextRequest) {
  return withApiAuth(request, async (user) => {
    const [sharedExpensesResult, sharedWithMeResult, balances, paymentHistory] = await Promise.all([
      getSharedExpenses(user.userId),
      getExpensesSharedWithMe(user.userId),
      getSettlementBalance(user.userId),
      getPaymentHistory(user.userId),
    ])

    return successResponse({
      sharedExpenses: sharedExpensesResult.items.map(formatSharedExpense),
      expensesSharedWithMe: sharedWithMeResult.items.map(formatParticipation),
      settlementBalances: balances.map(formatSettlementBalance),
      paymentHistory: paymentHistory.map((entry) => ({
        participantId: entry.participantId,
        userDisplayName: entry.userDisplayName,
        userEmail: entry.userEmail,
        amount: entry.amount.toString(),
        currency: entry.currency,
        paidAt: entry.paidAt.toISOString(),
        direction: entry.direction,
      })),
    })
  })
}
