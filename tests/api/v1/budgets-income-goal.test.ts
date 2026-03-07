import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { resetAllRateLimits } from '@/lib/rate-limit'
import { NextRequest } from 'next/server'
import {
  GET as GetIncomeGoal,
  POST as UpsertIncomeGoal,
  DELETE as DeleteIncomeGoal,
} from '@/app/api/v1/budgets/income-goal/route'
import { generateAccessToken } from '@/lib/jwt'
import { resetEnvCache } from '@/lib/env-schema'
import { prisma } from '@/lib/prisma'
import { getMonthStartFromKey } from '@/utils/date'
import { getApiTestUser, getOtherTestUser, TEST_USER_ID } from './helpers'

describe('Budget Income Goal API Routes', () => {
  let validToken: string
  let accountId: string
  let otherAccountId: string
  let incomeCategoryId: string
  const testMonthKey = '2024-01'
  const testMonth = getMonthStartFromKey(testMonthKey)

  beforeEach(async () => {
    process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing!'
    resetEnvCache()
    await resetAllRateLimits()
    validToken = generateAccessToken(TEST_USER_ID, 'api-test@example.com')

    const testUser = await getApiTestUser()
    const otherTestUser = await getOtherTestUser()

    const account = await prisma.account.upsert({
      where: { userId_name: { userId: testUser.id, name: 'TestAccount' } },
      update: {},
      create: { userId: testUser.id, name: 'TestAccount', type: 'SELF' },
    })
    const otherAccount = await prisma.account.upsert({
      where: { userId_name: { userId: otherTestUser.id, name: 'OtherAccount' } },
      update: {},
      create: { userId: otherTestUser.id, name: 'OtherAccount', type: 'SELF' },
    })
    const incomeCategory = await prisma.category.upsert({
      where: { userId_name_type: { userId: testUser.id, name: 'TEST_IncomeCategory', type: 'INCOME' } },
      update: {},
      create: { userId: testUser.id, name: 'TEST_IncomeCategory', type: 'INCOME' },
    })

    accountId = account.id
    otherAccountId = otherAccount.id
    incomeCategoryId = incomeCategory.id
  })

  afterEach(async () => {
    await prisma.monthlyIncomeGoal.deleteMany({
      where: { month: testMonth },
    })
    await prisma.transaction.deleteMany({
      where: {
        accountId,
        month: testMonth,
        categoryId: incomeCategoryId,
      },
    })
    await prisma.category.deleteMany({
      where: { name: 'TEST_IncomeCategory' },
    })
    await prisma.account.update({
      where: { id: accountId },
      data: {
        defaultIncomeGoal: null,
        defaultIncomeGoalCurrency: null,
      },
    })
  })

  describe('POST /api/v1/budgets/income-goal', () => {
    it('creates monthly income goal with valid JWT', async () => {
      const request = new NextRequest('http://localhost/api/v1/budgets/income-goal', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${validToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          accountId,
          monthKey: testMonthKey,
          amount: 4000,
          currency: 'USD',
          notes: 'Target income',
          setAsDefault: true,
        }),
      })

      const response = await UpsertIncomeGoal(request)
      const data = await response.json()

      expect(response.status).toBe(201)
      expect(data.success).toBe(true)
      expect(data.data.amount).toBe('4000')
      expect(data.data.currency).toBe('USD')
      expect(data.data.setAsDefault).toBe(true)

      const saved = await prisma.monthlyIncomeGoal.findFirst({
        where: { accountId, month: testMonth, deletedAt: null },
      })
      expect(saved).not.toBeNull()
      expect(saved?.amount.toNumber()).toBe(4000)
    })

    it('returns 403 for unauthorized account', async () => {
      const request = new NextRequest('http://localhost/api/v1/budgets/income-goal', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${validToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          accountId: otherAccountId,
          monthKey: testMonthKey,
          amount: 2500,
          currency: 'USD',
        }),
      })

      const response = await UpsertIncomeGoal(request)
      expect(response.status).toBe(403)
    })
  })

  describe('GET /api/v1/budgets/income-goal', () => {
    it('returns month-specific goal and actual income progress', async () => {
      await prisma.monthlyIncomeGoal.create({
        data: {
          accountId,
          month: testMonth,
          amount: 5000,
          currency: 'USD',
        },
      })
      await prisma.transaction.createMany({
        data: [
          {
            accountId,
            categoryId: incomeCategoryId,
            type: 'INCOME',
            amount: 2000,
            currency: 'USD',
            date: new Date('2024-01-10T00:00:00.000Z'),
            month: testMonth,
            description: 'Salary part 1',
          },
          {
            accountId,
            categoryId: incomeCategoryId,
            type: 'INCOME',
            amount: 1500,
            currency: 'USD',
            date: new Date('2024-01-21T00:00:00.000Z'),
            month: testMonth,
            description: 'Salary part 2',
          },
        ],
      })

      const request = new NextRequest(
        `http://localhost/api/v1/budgets/income-goal?accountId=${accountId}&monthKey=${testMonthKey}`,
        {
          method: 'GET',
          headers: { Authorization: `Bearer ${validToken}` },
        },
      )

      const response = await GetIncomeGoal(request)
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
      expect(data.data.incomeGoal.amount).toBe('5000')
      expect(data.data.incomeGoal.isDefault).toBe(false)
      expect(parseFloat(data.data.actualIncome)).toBeCloseTo(3500, 6)
    })

    it('falls back to account default when month goal is missing', async () => {
      await prisma.account.update({
        where: { id: accountId },
        data: {
          defaultIncomeGoal: 3200,
          defaultIncomeGoalCurrency: 'EUR',
        },
      })

      const request = new NextRequest(
        `http://localhost/api/v1/budgets/income-goal?accountId=${accountId}&monthKey=${testMonthKey}`,
        {
          method: 'GET',
          headers: { Authorization: `Bearer ${validToken}` },
        },
      )

      const response = await GetIncomeGoal(request)
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
      expect(data.data.incomeGoal.amount).toBe('3200')
      expect(data.data.incomeGoal.currency).toBe('EUR')
      expect(data.data.incomeGoal.isDefault).toBe(true)
    })

    it('returns 401 when token is missing', async () => {
      const request = new NextRequest(
        `http://localhost/api/v1/budgets/income-goal?accountId=${accountId}&monthKey=${testMonthKey}`,
        {
          method: 'GET',
        },
      )

      const response = await GetIncomeGoal(request)
      expect(response.status).toBe(401)
    })
  })

  describe('DELETE /api/v1/budgets/income-goal', () => {
    it('soft deletes month-specific income goal', async () => {
      await prisma.monthlyIncomeGoal.create({
        data: {
          accountId,
          month: testMonth,
          amount: 4200,
          currency: 'USD',
        },
      })

      const request = new NextRequest(
        `http://localhost/api/v1/budgets/income-goal?accountId=${accountId}&monthKey=${testMonthKey}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${validToken}` },
        },
      )

      const response = await DeleteIncomeGoal(request)
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.success).toBe(true)
      expect(data.data.deleted).toBe(true)

      const deleted = await prisma.monthlyIncomeGoal.findFirst({
        where: { accountId, month: testMonth },
      })
      expect(deleted?.deletedAt).toBeTruthy()
    })

    it('returns 404 when month goal does not exist', async () => {
      const request = new NextRequest(
        `http://localhost/api/v1/budgets/income-goal?accountId=${accountId}&monthKey=${testMonthKey}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${validToken}` },
        },
      )

      const response = await DeleteIncomeGoal(request)
      expect(response.status).toBe(404)
    })
  })
})
