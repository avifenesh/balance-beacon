import { Prisma, Currency, TransactionType } from '@prisma/client'
import { prisma } from '@/lib/prisma'
import { toDecimalString } from '@/utils/decimal'

export interface UpsertBudgetInput {
  accountId: string
  categoryId: string
  month: Date
  planned: number
  currency: Currency
  notes?: string | null
}

export interface DeleteBudgetInput {
  accountId: string
  categoryId: string
  month: Date
  userId: string
}

/**
 * Upsert a budget (create or update based on unique constraint)
 * If a soft-deleted budget exists, it will be restored.
 */
export async function upsertBudget(input: UpsertBudgetInput) {
  return await prisma.budget.upsert({
    where: {
      accountId_categoryId_month: {
        accountId: input.accountId,
        categoryId: input.categoryId,
        month: input.month,
      },
    },
    update: {
      planned: new Prisma.Decimal(toDecimalString(input.planned)),
      currency: input.currency,
      notes: input.notes ?? null,
      deletedAt: null, // Clear soft delete on update (restore if previously deleted)
      deletedBy: null,
    },
    create: {
      accountId: input.accountId,
      categoryId: input.categoryId,
      month: input.month,
      planned: new Prisma.Decimal(toDecimalString(input.planned)),
      currency: input.currency,
      notes: input.notes ?? null,
    },
  })
}

/**
 * Soft delete a budget by composite key
 */
export async function deleteBudget(input: DeleteBudgetInput) {
  return await prisma.budget.update({
    where: {
      accountId_categoryId_month: {
        accountId: input.accountId,
        categoryId: input.categoryId,
        month: input.month,
      },
      deletedAt: null, // Only delete non-deleted budgets
    },
    data: {
      deletedAt: new Date(),
      deletedBy: input.userId,
    },
  })
}

export interface GetBudgetByKeyInput {
  accountId: string
  categoryId: string
  month: Date
}

/**
 * Get a budget by composite key (excludes soft-deleted budgets)
 */
export async function getBudgetByKey(input: GetBudgetByKeyInput) {
  return await prisma.budget.findFirst({
    where: {
      accountId: input.accountId,
      categoryId: input.categoryId,
      month: input.month,
      deletedAt: null,
    },
  })
}

export interface UpsertMonthlyIncomeGoalInput {
  accountId: string
  month: Date
  amount: number
  currency: Currency
  notes?: string | null
  setAsDefault?: boolean
}

export interface DeleteMonthlyIncomeGoalInput {
  accountId: string
  month: Date
  userId: string
}

export interface GetMonthlyIncomeGoalByKeyInput {
  accountId: string
  month: Date
}

/**
 * Create/update a monthly income goal for an account and month.
 * If requested, also updates the account default income goal.
 */
export async function upsertMonthlyIncomeGoal(input: UpsertMonthlyIncomeGoalInput) {
  return await prisma.$transaction(async (tx) => {
    const goal = await tx.monthlyIncomeGoal.upsert({
      where: {
        accountId_month: {
          accountId: input.accountId,
          month: input.month,
        },
      },
      update: {
        amount: new Prisma.Decimal(toDecimalString(input.amount)),
        currency: input.currency,
        notes: input.notes ?? null,
        deletedAt: null,
        deletedBy: null,
      },
      create: {
        accountId: input.accountId,
        month: input.month,
        amount: new Prisma.Decimal(toDecimalString(input.amount)),
        currency: input.currency,
        notes: input.notes ?? null,
      },
    })

    if (input.setAsDefault) {
      await tx.account.update({
        where: { id: input.accountId },
        data: {
          defaultIncomeGoal: new Prisma.Decimal(toDecimalString(input.amount)),
          defaultIncomeGoalCurrency: input.currency,
        },
      })
    }

    return goal
  })
}

/**
 * Soft delete a monthly income goal by composite key.
 */
export async function deleteMonthlyIncomeGoal(input: DeleteMonthlyIncomeGoalInput) {
  return await prisma.monthlyIncomeGoal.update({
    where: {
      accountId_month: {
        accountId: input.accountId,
        month: input.month,
      },
      deletedAt: null,
    },
    data: {
      deletedAt: new Date(),
      deletedBy: input.userId,
    },
  })
}

/**
 * Get a monthly income goal for a specific account/month (excluding soft-deleted rows).
 */
export async function getMonthlyIncomeGoalByKey(input: GetMonthlyIncomeGoalByKeyInput) {
  return await prisma.monthlyIncomeGoal.findFirst({
    where: {
      accountId: input.accountId,
      month: input.month,
      deletedAt: null,
    },
  })
}

/**
 * Calculate actual income posted for the account in the selected month.
 */
export async function getActualIncomeForMonth(accountId: string, month: Date) {
  const aggregate = await prisma.transaction.aggregate({
    _sum: { amount: true },
    where: {
      accountId,
      month,
      type: TransactionType.INCOME,
      deletedAt: null,
    },
  })

  return aggregate._sum.amount ?? new Prisma.Decimal(0)
}
