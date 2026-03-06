import { describe, expect, it } from 'vitest'
import { TransactionType, Currency } from '@prisma/client'
import {
  recurringTemplateSchema,
  categorySchema,
  holdingSchema,
  resetPasswordSchema,
  transactionSchema,
  budgetSchema,
  monthlyIncomeGoalSchema,
  setBalanceSchema,
  applyRecurringSchema,
  transactionRequestSchema,
  participantSchema,
  createInitialCategoriesSchema,
  createQuickBudgetSchema,
} from '@/schemas'
import { DECIMAL_12_2_MAX } from '@/schemas/shared'

/**
 * Schema Edge Case Tests - Unique Coverage
 *
 * This file contains schema validation tests for edge cases NOT covered
 * in other test files:
 * - Recurring Template: dayOfMonth bounds, endMonthKey validation
 * - Category: name whitespace validation
 * - Holding: symbol format, quantity bounds
 * - Reset Password: token and password validation
 *
 * Tests for Transaction and Budget schemas are in:
 * - tests/transaction-edge-cases.test.ts
 * - tests/budget-edge-cases.test.ts
 *
 * Tests for Registration/Login schemas are in:
 * - tests/api/v1/auth-register.test.ts
 * - tests/api/v1/auth-password-reset.test.ts
 *
 * monthKey format validation (YYYY-MM with 01-12 range)
 * Amount upper bounds (Decimal(12,2) = max 9999999999.99)
 * Onboarding category name validation parity
 */

describe('Recurring Template Schema Edge Cases', () => {
  describe('dayOfMonth validation', () => {
    it('should accept day 1 (minimum)', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 1,
        startMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should accept day 31 (maximum)', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 31,
        startMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject day 0', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 0,
        startMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject day 32', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 32,
        startMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should coerce string dayOfMonth to number', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: '15',
        startMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
      if (result.success) {
        expect(result.data.dayOfMonth).toBe(15)
      }
    })
  })

  describe('endMonthKey validation', () => {
    it('should accept endMonthKey equal to startMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 15,
        startMonthKey: '2024-01',
        endMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should accept endMonthKey after startMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 15,
        startMonthKey: '2024-01',
        endMonthKey: '2024-12',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject endMonthKey before startMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 15,
        startMonthKey: '2024-06',
        endMonthKey: '2024-01',
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should accept null endMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        type: TransactionType.EXPENSE,
        amount: 50,
        currency: Currency.USD,
        dayOfMonth: 15,
        startMonthKey: '2024-01',
        endMonthKey: null,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })
  })
})

describe('Category Schema Edge Cases', () => {
  describe('name validation', () => {
    it('should accept 2 character name (minimum)', () => {
      const result = categorySchema.safeParse({
        name: 'AB',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject 1 character name', () => {
      const result = categorySchema.safeParse({
        name: 'A',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should accept 100 character name (maximum)', () => {
      const result = categorySchema.safeParse({
        name: 'A' + 'b'.repeat(98) + 'C',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject 101 character name', () => {
      const result = categorySchema.safeParse({
        name: 'a'.repeat(101),
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should accept alphanumeric name', () => {
      const result = categorySchema.safeParse({
        name: 'Food123',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should accept name with spaces in middle', () => {
      const result = categorySchema.safeParse({
        name: 'Fast Food',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject name starting with space', () => {
      const result = categorySchema.safeParse({
        name: ' Food',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject name ending with space', () => {
      const result = categorySchema.safeParse({
        name: 'Food ',
        type: TransactionType.EXPENSE,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })
})

describe('Holding Schema Edge Cases', () => {
  describe('symbol validation', () => {
    it('should accept 1 character symbol', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'A',
        quantity: 10,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should accept 5 character symbol (maximum)', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'GOOGL',
        quantity: 10,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject 6 character symbol', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'ABCDEF',
        quantity: 10,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject lowercase symbol', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'aapl',
        quantity: 10,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject symbol with numbers', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAP1',
        quantity: 10,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })

  describe('quantity validation', () => {
    it('should accept minimum quantity (0.000001)', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAPL',
        quantity: 0.000001,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should accept maximum quantity (999999999)', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAPL',
        quantity: 999999999,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject quantity exceeding maximum', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAPL',
        quantity: 1000000000,
        averageCost: 100,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })

  describe('averageCost validation', () => {
    it('should accept zero averageCost', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAPL',
        quantity: 10,
        averageCost: 0,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject negative averageCost', () => {
      const result = holdingSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        symbol: 'AAPL',
        quantity: 10,
        averageCost: -50,
        currency: Currency.USD,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })
})

describe('Reset Password Schema Edge Cases', () => {
  it('should accept valid token and password', () => {
    const result = resetPasswordSchema.safeParse({
      token: 'valid-reset-token',
      newPassword: 'NewPassword1',
      csrfToken: 'valid-token',
    })
    expect(result.success).toBe(true)
  })

  it('should reject empty token', () => {
    const result = resetPasswordSchema.safeParse({
      token: '',
      newPassword: 'NewPassword1',
      csrfToken: 'valid-token',
    })
    expect(result.success).toBe(false)
  })

  it('should reject weak password', () => {
    const result = resetPasswordSchema.safeParse({
      token: 'valid-reset-token',
      newPassword: 'weak',
      csrfToken: 'valid-token',
    })
    expect(result.success).toBe(false)
  })
})

describe('monthKey Format Validation', () => {
  const validBase = {
    accountId: 'acc-123',
    categoryId: 'cat-123',
    csrfToken: 'token-123',
  }

  describe('budgetSchema monthKey', () => {
    it('should accept valid YYYY-MM format', () => {
      const result = budgetSchema.safeParse({
        ...validBase,
        monthKey: '2024-01',
        planned: 500,
      })
      expect(result.success).toBe(true)
    })

    it('should reject invalid month 13', () => {
      const result = budgetSchema.safeParse({
        ...validBase,
        monthKey: '2024-13',
        planned: 500,
      })
      expect(result.success).toBe(false)
    })

    it('should reject month 00', () => {
      const result = budgetSchema.safeParse({
        ...validBase,
        monthKey: '2024-00',
        planned: 500,
      })
      expect(result.success).toBe(false)
    })

    it('should reject missing leading zero', () => {
      const result = budgetSchema.safeParse({
        ...validBase,
        monthKey: '2024-1',
        planned: 500,
      })
      expect(result.success).toBe(false)
    })

    it('should reject non-numeric format', () => {
      const result = budgetSchema.safeParse({
        ...validBase,
        monthKey: 'Jan-2024',
        planned: 500,
      })
      expect(result.success).toBe(false)
    })
  })

  describe('monthlyIncomeGoalSchema monthKey', () => {
    it('should accept valid month', () => {
      const result = monthlyIncomeGoalSchema.safeParse({
        ...validBase,
        monthKey: '2024-12',
        amount: 5000,
      })
      expect(result.success).toBe(true)
    })

    it('should reject invalid month', () => {
      const result = monthlyIncomeGoalSchema.safeParse({
        ...validBase,
        monthKey: '2024-99',
        amount: 5000,
      })
      expect(result.success).toBe(false)
    })
  })

  describe('setBalanceSchema monthKey', () => {
    it('should accept valid month', () => {
      const result = setBalanceSchema.safeParse({
        ...validBase,
        monthKey: '2024-06',
        targetBalance: 1000,
      })
      expect(result.success).toBe(true)
    })

    it('should reject invalid month', () => {
      const result = setBalanceSchema.safeParse({
        ...validBase,
        monthKey: '2024-13',
        targetBalance: 1000,
      })
      expect(result.success).toBe(false)
    })
  })

  describe('applyRecurringSchema monthKey', () => {
    it('should accept valid month', () => {
      const result = applyRecurringSchema.safeParse({
        ...validBase,
        monthKey: '2024-01',
      })
      expect(result.success).toBe(true)
    })

    it('should reject invalid month', () => {
      const result = applyRecurringSchema.safeParse({
        ...validBase,
        monthKey: '2024-00',
      })
      expect(result.success).toBe(false)
    })
  })

  describe('recurringTemplateSchema monthKey', () => {
    const recurringBase = {
      accountId: 'acc-123',
      categoryId: 'cat-123',
      type: TransactionType.EXPENSE,
      amount: 50,
      currency: Currency.USD,
      dayOfMonth: 15,
      csrfToken: 'token-123',
    }

    it('should reject invalid startMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        ...recurringBase,
        startMonthKey: '2024-13',
      })
      expect(result.success).toBe(false)
    })

    it('should reject invalid endMonthKey', () => {
      const result = recurringTemplateSchema.safeParse({
        ...recurringBase,
        startMonthKey: '2024-01',
        endMonthKey: '2024-00',
      })
      expect(result.success).toBe(false)
    })
  })
})

describe('Amount Upper Bound Validation (Decimal(12,2))', () => {
  const MAX = DECIMAL_12_2_MAX

  it('transactionSchema should reject amount exceeding Decimal(12,2)', () => {
    const result = transactionSchema.safeParse({
      accountId: 'acc-123',
      categoryId: 'cat-123',
      type: TransactionType.EXPENSE,
      amount: MAX + 1,
      currency: Currency.USD,
      date: new Date(),
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('transactionSchema should accept amount at Decimal(12,2) limit', () => {
    const result = transactionSchema.safeParse({
      accountId: 'acc-123',
      categoryId: 'cat-123',
      type: TransactionType.EXPENSE,
      amount: MAX,
      currency: Currency.USD,
      date: new Date(),
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(true)
  })

  it('transactionRequestSchema should reject oversized amount', () => {
    const result = transactionRequestSchema.safeParse({
      toId: 'acc-456',
      categoryId: 'cat-123',
      amount: MAX + 1,
      date: new Date(),
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('budgetSchema should reject oversized planned amount', () => {
    const result = budgetSchema.safeParse({
      accountId: 'acc-123',
      categoryId: 'cat-123',
      monthKey: '2024-01',
      planned: MAX + 1,
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('monthlyIncomeGoalSchema should reject oversized amount', () => {
    const result = monthlyIncomeGoalSchema.safeParse({
      accountId: 'acc-123',
      monthKey: '2024-01',
      amount: MAX + 1,
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('setBalanceSchema should reject positive amount exceeding limit', () => {
    const result = setBalanceSchema.safeParse({
      accountId: 'acc-123',
      targetBalance: MAX + 1,
      monthKey: '2024-01',
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('setBalanceSchema should reject negative amount exceeding limit', () => {
    const result = setBalanceSchema.safeParse({
      accountId: 'acc-123',
      targetBalance: -(MAX + 1),
      monthKey: '2024-01',
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('setBalanceSchema should accept negative balance within limit', () => {
    const result = setBalanceSchema.safeParse({
      accountId: 'acc-123',
      targetBalance: -5000,
      monthKey: '2024-01',
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(true)
  })

  it('holdingSchema should reject oversized averageCost', () => {
    const result = holdingSchema.safeParse({
      accountId: 'acc-123',
      categoryId: 'cat-123',
      symbol: 'AAPL',
      quantity: 10,
      averageCost: MAX + 1,
      currency: Currency.USD,
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })

  it('participantSchema should reject oversized shareAmount', () => {
    const result = participantSchema.safeParse({
      email: 'test@example.com',
      shareAmount: MAX + 1,
    })
    expect(result.success).toBe(false)
  })

  it('recurringTemplateSchema should reject oversized amount', () => {
    const result = recurringTemplateSchema.safeParse({
      accountId: 'acc-123',
      categoryId: 'cat-123',
      type: TransactionType.EXPENSE,
      amount: MAX + 1,
      dayOfMonth: 15,
      startMonthKey: '2024-01',
      csrfToken: 'token-123',
    })
    expect(result.success).toBe(false)
  })
})

describe('Onboarding Schema Parity', () => {
  describe('createInitialCategoriesSchema name validation', () => {
    it('should accept valid category name', () => {
      const result = createInitialCategoriesSchema.safeParse({
        categories: [{ name: 'Groceries', type: TransactionType.EXPENSE }],
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject category name starting with space', () => {
      const result = createInitialCategoriesSchema.safeParse({
        categories: [{ name: ' Groceries', type: TransactionType.EXPENSE }],
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject category name exceeding 100 chars', () => {
      const result = createInitialCategoriesSchema.safeParse({
        categories: [{ name: 'A'.repeat(101), type: TransactionType.EXPENSE }],
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject 1 character category name', () => {
      const result = createInitialCategoriesSchema.safeParse({
        categories: [{ name: 'A', type: TransactionType.EXPENSE }],
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })

  describe('createQuickBudgetSchema monthKey', () => {
    it('should accept valid YYYY-MM', () => {
      const result = createQuickBudgetSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        monthKey: '2024-06',
        planned: 500,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(true)
    })

    it('should reject invalid month 13', () => {
      const result = createQuickBudgetSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        monthKey: '2024-13',
        planned: 500,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })

    it('should reject oversized planned amount', () => {
      const result = createQuickBudgetSchema.safeParse({
        accountId: 'acc-123',
        categoryId: 'cat-123',
        monthKey: '2024-01',
        planned: 10000000000,
        csrfToken: 'token-123',
      })
      expect(result.success).toBe(false)
    })
  })
})
